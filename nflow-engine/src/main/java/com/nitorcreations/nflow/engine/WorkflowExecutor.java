package com.nitorcreations.nflow.engine;

import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import com.nitorcreations.nflow.engine.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

public class WorkflowExecutor implements Runnable {

  private static final Logger logger = getLogger(WorkflowExecutor.class);

  private final int MAX_SUBSEQUENT_STATE_EXECUTIONS = 100;

  private final Integer instanceId;
  private final RepositoryService repository;

  private static final String MDC_KEY = "workflowInstanceId";
  private final List<WorkflowExecutorListener> executorListeners;

  public WorkflowExecutor(Integer instanceId, RepositoryService repository,
      WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.repository = repository;
    this.executorListeners = Arrays.asList(executorListeners);
  }

  @Override
  public void run() {
    try {
      MDC.put(MDC_KEY, String.valueOf(instanceId));
      runImpl();
    } catch (Exception ex) {
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
      logger.warn("Execution lagging " + executionLag.getStandardSeconds()
          + " seconds.");
    }
    WorkflowDefinition<? extends WorkflowState> definition = repository
        .getWorkflowDefinition(instance.type);
    if (definition == null) {
      logger
          .warn("Workflow type %s not configured to this nflow instance - unscheduling workflow instance");
      instance = new WorkflowInstance.Builder(instance).setNextActivation(null)
          .setStateText("Unsupported workflow type").build();
      repository.updateWorkflowInstance(instance, true);
      logger.debug("Exiting.");
      return;
    }

    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.currentlyProcessing) {
      subsequentStateExecutions++;
      StateExecutionImpl execution = new StateExecutionImpl(instance);
      ListenerContext listenerContext = new ListenerContext(definition,
          instance, execution);
      try {
        processBeforeListeners(listenerContext);
        processState(instance, definition, execution);
        processAfterListeners(listenerContext);
      } catch (Exception ex) {
        logger.error("Handler threw exception, trying again later", ex);
        execution.setNextActivation(now().plusMillis(
            settings.getErrorBumpedTransitionDelay()));
        processAfterFailureListeners(listenerContext, ex);
      } finally {
        if (subsequentStateExecutions++ >= MAX_SUBSEQUENT_STATE_EXECUTIONS
            && execution.getNextActivation() != null) {
          logger.warn("Executed " + MAX_SUBSEQUENT_STATE_EXECUTIONS
              + " times without delay, forcing short transition delay");
          execution.setNextActivation(execution.getNextActivation().plusMillis(
              settings.getShortTransitionDelay()));
        }
        instance = new WorkflowInstance.Builder(instance)
            .setNextActivation(execution.getNextActivation())
            .setState(execution.getNextState())
            .setStateText(execution.getNextStateReason())
            .setCurrentlyProcessing(
                !now().isBefore(execution.getNextActivation())
                    && execution.getNextActivation() != null).build();
        repository.updateWorkflowInstance(instance, execution.isSaveTrace());
      }
    }
    logger.debug("Finished.");
  }

  private void processState(WorkflowInstance instance,
      WorkflowDefinition<?> definition, StateExecutionImpl execution) {
    Method stateHandler = ReflectionUtils.findMethod(definition.getClass(),
        instance.state, StateExecution.class);
    ReflectionUtils.invokeMethod(stateHandler, definition, execution);
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
