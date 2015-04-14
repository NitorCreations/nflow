package com.nitorcreations.nflow.engine.internal.executor;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.joda.time.DateTime.now;
import static org.joda.time.Duration.standardMinutes;
import static org.joda.time.Duration.standardSeconds;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
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
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

class WorkflowStateProcessor implements Runnable {

  private static final Logger logger = getLogger(WorkflowStateProcessor.class);
  private static final String MDC_KEY = "workflowInstanceId";

  private final int MAX_SUBSEQUENT_STATE_EXECUTIONS = 100;

  private final int instanceId;
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final WorkflowExecutorListener[] executorListeners;
  private final String illegalStateChangeAction;
  DateTime lastLogged = now();
  private final int unknownWorkflowTypeRetryDelay;
  private final int unknownWorkflowStateRetryDelay;

  WorkflowStateProcessor(int instanceId, ObjectStringMapper objectMapper, WorkflowDefinitionService workflowDefinitions,
      WorkflowInstanceService workflowInstances, WorkflowInstanceDao workflowInstanceDao, Environment env,
      WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.objectMapper = objectMapper;
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.workflowInstanceDao = workflowInstanceDao;
    this.executorListeners = executorListeners;
    illegalStateChangeAction = env.getRequiredProperty("nflow.illegal.state.change.action");
    unknownWorkflowTypeRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.type.retry.delay.minutes", Integer.class);
    unknownWorkflowStateRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.state.retry.delay.minutes", Integer.class);

  }

  @Override
  public void run() {
    try {
      MDC.put(MDC_KEY, String.valueOf(instanceId));
      runImpl();
    } catch (Throwable ex) {
      logger.error("Unexpected failure occurred", ex);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private void runImpl() {
    logger.debug("Starting.");
    WorkflowInstance instance = workflowInstances.getWorkflowInstance(instanceId);
    logIfLagging(instance);
    WorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    if (definition == null) {
      rescheduleUnknownWorkflowType(instance);
      return;
    }
    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.status == executing) {
      StateExecutionImpl execution = new StateExecutionImpl(instance, objectMapper);
      ListenerContext listenerContext = executorListeners.length == 0 ? null : new ListenerContext(definition, instance, execution);
      WorkflowInstanceAction.Builder actionBuilder = new WorkflowInstanceAction.Builder(instance);
      WorkflowState state;
      try {
        state = definition.getState(instance.state);
      } catch (IllegalStateException e) {
        rescheduleUnknownWorkflowState(instance);
        return;
      }
      try {
        processBeforeListeners(listenerContext);
        NextAction nextAction = processState(instance, definition, execution, state);
        if (listenerContext != null) {
          listenerContext.nextAction = nextAction;
        }
      } catch (Throwable t) {
        execution.setFailed(t);
        logger.error("Handler threw exception, trying again later.", t);
        execution.setRetry(true);
        execution.setNextState(state);
        execution.setNextStateReason(getStackTrace(t));
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

  void logIfLagging(WorkflowInstance instance) {
    DateTime now = now();
    Duration executionLag = new Duration(instance.nextActivation, now);
    if (executionLag.isLongerThan(standardMinutes(1))) {
      Duration logInterval = new Duration(lastLogged, now);
      if (logInterval.isLongerThan(standardSeconds(30))) {
        logger.warn("Execution lagging {} seconds.", executionLag.getStandardSeconds());
        lastLogged = now;
      }
    }
  }

  private void rescheduleUnknownWorkflowType(WorkflowInstance instance) {
    logger.warn("Workflow type {} not configured to this nFlow instance - rescheduling workflow instance", instance.type);
    instance = new WorkflowInstance.Builder(instance).setNextActivation(now().plusMinutes(unknownWorkflowTypeRetryDelay))
        .setStatus(inProgress).setStateText("Unsupported workflow type").build();
    workflowInstanceDao.updateWorkflowInstance(instance);
    logger.debug("Finished.");
  }

  private void rescheduleUnknownWorkflowState(WorkflowInstance instance) {
    logger.warn("Workflow state {} not configured to workflow type {} - rescheduling workflow instance", instance.state,
        instance.type);
    instance = new WorkflowInstance.Builder(instance).setNextActivation(now().plusMinutes(unknownWorkflowStateRetryDelay))
        .setStatus(inProgress).setStateText("Unsupported workflow state").build();
    workflowInstanceDao.updateWorkflowInstance(instance);
    logger.debug("Finished.");
  }

  private int busyLoopPrevention(WorkflowSettings settings,
      int subsequentStateExecutions, StateExecutionImpl execution) {
    if (subsequentStateExecutions++ >= MAX_SUBSEQUENT_STATE_EXECUTIONS && execution.getNextActivation() != null) {
      logger.warn("Executed {} times without delay, forcing short transition delay", MAX_SUBSEQUENT_STATE_EXECUTIONS);
      if (execution.getNextActivation().isBefore(settings.getShortTransitionActivation())) {
        execution.setNextActivation(settings.getShortTransitionActivation());
      }
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
      .setStatus(getStatus(execution, definition.getState(execution.getNextState())))
      .setStateText(getStateText(instance, execution))
      .setState(execution.getNextState())
      .setRetries(execution.isRetry() ? execution.getRetries() + 1 : 0);
    actionBuilder.setExecutionEnd(now()).setType(getActionType(execution)).setStateText(execution.getNextStateReason());
    workflowInstanceDao.updateWorkflowInstanceAfterExecution(builder.build(), actionBuilder.build());
    return builder.setOriginalStateVariables(instance.stateVariables).build();
  }

  private String getStateText(WorkflowInstance instance, StateExecutionImpl execution) {
    if (execution.isRetry() || execution.isRetryCountExceeded()) {
      return execution.getNextStateReason();
    }
    if (execution.getNextActivation() == null) {
      return "Stopped in state " + execution.getNextState();
    }
    return "Scheduled by previous state " + instance.state;
  }

  private WorkflowInstanceStatus getStatus(StateExecutionImpl execution, WorkflowState nextState) {
    if (isNextActivationImmediately(execution)) {
      return executing;
    }
    if (nextState.getType() == manual) {
      return WorkflowInstanceStatus.manual;
    }
    if (nextState.getType().isFinal()) {
      return finished;
    }
    return inProgress;
  }

  private WorkflowActionType getActionType(StateExecutionImpl execution) {
    return execution.isFailed() || execution.isRetryCountExceeded() ? stateExecutionFailed : stateExecution;
  }

  private boolean isNextActivationImmediately(StateExecutionImpl execution) {
    return execution.getNextActivation() != null && !execution.getNextActivation().isAfterNow();
  }

  private NextAction processState(WorkflowInstance instance, WorkflowDefinition<?> definition, StateExecutionImpl execution,
      WorkflowState currentState) {
    WorkflowStateMethod method = definition.getMethod(instance.state);
    if (method == null) {
      execution.setNextState(currentState);
      return stopInState(currentState, "Execution finished.");
    }
    NextAction nextAction;
    Object[] args = objectMapper.createArguments(execution, method);
    if (currentState.getType().isFinal()) {
      invokeMethod(method.method, definition, args);
      nextAction = stopInState(currentState, "Stopped in final state");
    } else {
      try {
        nextAction = (NextAction) invokeMethod(method.method, definition, args);
        if (nextAction == null) {
          logger.error("State '{}' handler method returned null, proceeding to error state '{}'", instance.state, definition
              .getErrorState().name());
          nextAction = moveToState(definition.getErrorState(), "State handler method returned null");
          execution.setFailed();
        } else if (nextAction.getNextState() != null && !definition.getStates().contains(nextAction.getNextState())) {
          logger.error("State '{}' is not a state of '{}' workflow definition, proceeding to error state '{}'",
              nextAction.getNextState(), definition.getType(), definition.getErrorState().name());
          nextAction = moveToState(definition.getErrorState(), "State '" + instance.state
              + "' handler method returned invalid next state '" + nextAction.getNextState() + "'");
          execution.setFailed();
        } else if (!"ignore".equals(illegalStateChangeAction) && !definition.isAllowedNextAction(instance, nextAction)) {
          logger.warn("State transition from '{}' to '{}' is not allowed by workflow definition.", instance.state,
              nextAction.getNextState());
          if ("fail".equals(illegalStateChangeAction)) {
            nextAction = moveToState(definition.getErrorState(), "Illegal state transition from " + instance.state + " to "
                + nextAction.getNextState().name() + ", proceeding to error state " + definition.getErrorState().name());
            execution.setFailed();
          }
        }
      } catch (InvalidNextActionException e) {
        logger.error("State '" + instance.state
            + "' handler method failed to create valid next action, proceeding to error state '"
            + definition.getErrorState().name() + "'", e);
        nextAction = moveToState(definition.getErrorState(), e.getMessage());
        execution.setFailed(e);
      }
    }
    execution.setNextActivation(nextAction.getActivation());
    execution.setNextStateReason(nextAction.getReason());
    execution.setSaveTrace(nextAction.isSaveTrace());
    if (nextAction.isRetry()) {
      execution.setNextState(currentState);
      execution.setRetry(true);
      definition.handleRetryAfter(execution, nextAction.getActivation());
    } else {
      execution.setNextState(nextAction.getNextState());
    }
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
