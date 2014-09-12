package com.nitorcreations.nflow.engine.internal.executor;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

class WorkflowExecutor implements Runnable {

  private static final Logger logger = getLogger(WorkflowExecutor.class);
  private static final String MDC_KEY = "workflowInstanceId";

  private final int MAX_SUBSEQUENT_STATE_EXECUTIONS = 100;

  private final int instanceId;
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowExecutorListener[] executorListeners;

  WorkflowExecutor(int instanceId, ObjectStringMapper objectMapper, WorkflowDefinitionService workflowDefinitions,
      WorkflowInstanceService workflowInstances, WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.objectMapper = objectMapper;
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.executorListeners = executorListeners;
  }

  @Override
  public void run() {
    try {
      MDC.put(MDC_KEY, String.valueOf(instanceId));
      runImpl();
    } catch (Throwable ex) {
      logger.error("Totally unexpected failure (e.g. deadlock) occurred (" + ex.getMessage() + ")", ex);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private void runImpl() {
    logger.debug("Starting.");
    WorkflowInstance instance = workflowInstances.getWorkflowInstance(instanceId);
    Duration executionLag = new Duration(instance.nextActivation, null);
    if (executionLag.isLongerThan(Duration.standardMinutes(1))) {
      logger.warn("Execution lagging {} seconds.", executionLag.getStandardSeconds());
    }
    WorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    if (definition == null) {
      unscheduleUnknownWorkflowInstance(instance);
      return;
    }
    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.processing) {
      StateExecutionImpl execution = new StateExecutionImpl(instance, objectMapper);
      ListenerContext listenerContext = executorListeners.length == 0 ? null : new ListenerContext(definition, instance, execution);
      WorkflowInstanceAction.Builder actionBuilder = new WorkflowInstanceAction.Builder(instance);
      WorkflowState state = definition.getState(instance.state);
      try {
        processBeforeListeners(listenerContext);
        NextAction nextAction = processState(instance, definition, execution);
        if (listenerContext != null) {
          listenerContext.nextAction = nextAction;
        }
      } catch (Throwable t) {
        execution.setFailed(t);
        logger.error("Handler threw exception, trying again later (" + t.getMessage() + ")", t);
        execution.setRetry(true);
        execution.setNextState(state);
        execution.setNextStateReason(t.toString());
        definition.handleRetry(execution);
      } finally {
        if (execution.isFailed()) {
          processAfterFailureListeners(listenerContext, execution.getThrown());
        } else {
          processAfterListeners(listenerContext);
        }
        subsequentStateExecutions = busyLoopPrevention(settings, subsequentStateExecutions, execution);
        instance = saveWorkflowInstanceState(execution, instance, definition, actionBuilder);
      }
    }
    logger.debug("Finished.");
  }

  private void unscheduleUnknownWorkflowInstance(WorkflowInstance instance) {
    logger.warn("Workflow type {} not configured to this nflow instance - unscheduling workflow instance", instance.type);
    instance = new WorkflowInstance.Builder(instance).setNextActivation(null)
        .setStateText("Unsupported workflow type").build();
    workflowInstances.updateWorkflowInstance(instance, null);
    logger.debug("Exiting.");
  }

  private int busyLoopPrevention(WorkflowSettings settings,
      int subsequentStateExecutions, StateExecutionImpl execution) {
    if (subsequentStateExecutions++ >= MAX_SUBSEQUENT_STATE_EXECUTIONS && execution.getNextActivation() != null) {
      logger.warn("Executed {} times without delay, forcing short transition delay", MAX_SUBSEQUENT_STATE_EXECUTIONS);
      execution.setNextActivation(execution.getNextActivation().plusMillis(settings.getShortTransitionDelay()));
    }
    return subsequentStateExecutions;
  }

  private WorkflowInstance saveWorkflowInstanceState(StateExecutionImpl execution, WorkflowInstance instance,
      WorkflowDefinition<?> definition, WorkflowInstanceAction.Builder actionBuilder) {
    if (definition.getMethod(execution.getNextState()) == null && execution.getNextActivation() != null) {
      logger.info("No handler method defined for {}, clearing next activation", execution.getNextState());
      execution.setNextActivation(null);
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance)
      .setNextActivation(execution.getNextActivation())
      .setProcessing(isNextActivationImmediately(execution))
      .setStateText(execution.isRetry() ? execution.getNextStateReason() : null)
      .setState(execution.getNextState())
      .setRetries(execution.isRetry() ? execution.getRetries() + 1 : 0);
    actionBuilder.setExecutionEnd(now()).setStateText(execution.getNextStateReason());
    workflowInstances.updateWorkflowInstance(builder.build(), actionBuilder.build());
    return builder.setOriginalStateVariables(instance.stateVariables).build();
  }

  private boolean isNextActivationImmediately(StateExecutionImpl execution) {
    return execution.getNextActivation() != null && !execution.getNextActivation().isAfterNow();
  }

  private NextAction processState(WorkflowInstance instance, WorkflowDefinition<?> definition, StateExecutionImpl execution) {
    WorkflowStateMethod method = definition.getMethod(instance.state);
    Object[] args = objectMapper.createArguments(execution, method);
    NextAction nextAction;
    try {
      nextAction = (NextAction) invokeMethod(method.method, definition, args);
    } catch (InvalidNextActionException e) {
      logger.error("State '" + instance.state
          + "' handler method failed to create valid next action, proceeding to error state '"
          + definition.getErrorState().name() + "'", e);
      nextAction = moveToState(definition.getErrorState(), e.getMessage());
      execution.setFailed(e);
    }
    if (nextAction == null) {
      logger.error("State '{}' handler method returned null, proceeding to error state '{}'",
          instance.state, definition.getErrorState().name());
      nextAction = moveToState(definition.getErrorState(), "State handler method returned null");
      execution.setFailed();
    }
    execution.setNextActivation(nextAction.getActivation());
    if (nextAction.getNextState() == null) {
      execution.setNextState(definition.getState(instance.state));
      execution.setRetry(true);
    } else {
      execution.setNextState(nextAction.getNextState());
    }
    execution.setNextStateReason(nextAction.getReason());
    execution.setSaveTrace(nextAction.isSaveTrace());
    objectMapper.storeArguments(execution, method, args);
    return nextAction;
  }

  private void processBeforeListeners(ListenerContext listenerContext) {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.beforeProcessing(listenerContext);
      } catch (Throwable t) {
        logger.error("Error in " + listener.getClass().getName() + ".beforeProcessing (" + t.getMessage() + ")", t);
      }
    }
  }

  private void processAfterListeners(ListenerContext listenerContext) {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.afterProcessing(listenerContext);
      } catch (Throwable t) {
        logger.error("Error in " + listener.getClass().getName() + ".afterProcessing (" + t.getMessage() + ")", t);
      }
    }
  }

  private void processAfterFailureListeners(ListenerContext listenerContext, Throwable ex) {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.afterFailure(listenerContext, ex);
      } catch (Throwable t) {
        logger.error("Error in " + listener.getClass().getName() + ".afterFailure (" + t.getMessage() + ")", t);
      }
    }
  }
}
