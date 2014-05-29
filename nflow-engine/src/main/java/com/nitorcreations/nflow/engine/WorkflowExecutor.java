package com.nitorcreations.nflow.engine;

import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import com.nitorcreations.nflow.engine.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;
import com.nitorcreations.nflow.engine.workflow.data.WorkflowStateMethod;

public class WorkflowExecutor implements Runnable {

  private static final Logger logger = getLogger(WorkflowExecutor.class);

  private final int MAX_SUBSEQUENT_STATE_EXECUTIONS = 100;

  private final int instanceId;
  private final RepositoryService repository;
  private final ObjectStringMapper objectMapper;

  private static final String MDC_KEY = "workflowInstanceId";
  private final WorkflowExecutorListener[] executorListeners;


  public WorkflowExecutor(int instanceId, ObjectStringMapper objectMapper, RepositoryService repository,
      WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.objectMapper = objectMapper;
    this.repository = repository;
    this.executorListeners = executorListeners;
  }

  @Override
  public void run() {
    try {
      MDC.put(MDC_KEY, String.valueOf(instanceId));
      runImpl();
    } catch (Throwable ex) {
      logger.error("Totally unexpected failure (e.g. deadlock) occurred.", ex);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private void runImpl() {
    logger.debug("Starting.");
    WorkflowInstance instance = repository.getWorkflowInstance(instanceId);
    Duration executionLag = new Duration(instance.nextActivation, null);
    if (executionLag.isLongerThan(Duration.standardMinutes(1))) {
      logger.warn("Execution lagging {} seconds.", executionLag.getStandardSeconds());
    }
    WorkflowDefinition<? extends WorkflowState> definition = repository.getWorkflowDefinition(instance.type);
    if (definition == null) {
      unscheduleUnknownWorkflowInstance(instance);
      return;
    }
    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.processing) {
      StateExecutionImpl execution = new StateExecutionImpl(instance, objectMapper);
      ListenerContext listenerContext = new ListenerContext(definition, instance, execution);
      WorkflowInstanceAction.Builder actionBuilder = new WorkflowInstanceAction.Builder(instance);
      try {
        processBeforeListeners(listenerContext);
        processState(instance, definition, execution);
        processAfterListeners(listenerContext);
      } catch (Exception ex) {
        logger.error("Handler threw exception, trying again later", ex);
        execution.setFailure(true);
        definition.handleRetry(execution);
        processAfterFailureListeners(listenerContext, ex);
      } finally {
        subsequentStateExecutions = busyLoopPrevention(settings, subsequentStateExecutions, execution);
        instance = saveWorkflowInstanceState(execution, instance, actionBuilder);
      }
    }
    logger.debug("Finished.");
  }

  private void unscheduleUnknownWorkflowInstance(WorkflowInstance instance) {
    logger.warn("Workflow type {} not configured to this nflow instance - unscheduling workflow instance", instance.type);
    instance = new WorkflowInstance.Builder(instance).setNextActivation(null)
        .setStateText("Unsupported workflow type").build();
    repository.updateWorkflowInstance(instance, null);
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

  private WorkflowInstance saveWorkflowInstanceState(StateExecutionImpl execution, WorkflowInstance instance, WorkflowInstanceAction.Builder actionBuilder) {
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance)
      .setNextActivation(execution.getNextActivation())
      .setProcessing(isNextActivationImmediately(execution));
    if (execution.isFailure()) {
      builder.setRetries(execution.getRetries() + 1);
    } else {
      builder.setState(execution.getNextState()).setStateText(execution.getNextStateReason()).setRetries(0);
    }
    actionBuilder.setExecutionEnd(now()).setStateText(execution.getNextStateReason());
    return repository.updateWorkflowInstance(builder.build(), actionBuilder.build());
  }

  private boolean isNextActivationImmediately(StateExecutionImpl execution) {
    return !now().isBefore(execution.getNextActivation()) && execution.getNextActivation() != null;
  }

  private void processState(WorkflowInstance instance,
      WorkflowDefinition<?> definition, StateExecutionImpl execution) {
    WorkflowStateMethod method = definition.getMethod(instance.state);
    Object[] args = objectMapper.createArguments(execution, method);
    ReflectionUtils.invokeMethod(method.method, definition, args);
    objectMapper.storeArguments(execution, method, args);
  }

  private void processBeforeListeners(ListenerContext listenerContext) {
    for (WorkflowExecutorListener listener : executorListeners) {
      listener.beforeProcessing(listenerContext);
    }
  }

  private void processAfterListeners(ListenerContext listenerContext) {
    for (WorkflowExecutorListener listener : executorListeners) {
      listener.afterProcessing(listenerContext);
    }
  }

  private void processAfterFailureListeners(ListenerContext listenerContext,
      Exception ex) {
    for (WorkflowExecutorListener listener : executorListeners) {
      listener.afterFailure(listenerContext, ex);
    }
  }

}
