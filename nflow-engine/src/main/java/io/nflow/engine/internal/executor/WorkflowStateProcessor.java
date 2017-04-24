package io.nflow.engine.internal.executor;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.Duration.standardMinutes;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.StateExecutionImpl;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.internal.workflow.WorkflowStateMethod;
import io.nflow.engine.listener.AbstractWorkflowExecutorListener;
import io.nflow.engine.listener.ListenerChain;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

class WorkflowStateProcessor implements Runnable {

  static final Logger logger = getLogger(WorkflowStateProcessor.class);
  private static final PeriodicLogger laggingLogger = new PeriodicLogger(logger, 30);
  private static final PeriodicLogger threadStuckLogger = new PeriodicLogger(logger, 60);
  private static final String MDC_KEY = "workflowInstanceId";

  private final int instanceId;
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final List<WorkflowExecutorListener> executorListeners;
  final String illegalStateChangeAction;
  private final int unknownWorkflowTypeRetryDelay;
  private final int unknownWorkflowStateRetryDelay;
  private final Map<Integer, WorkflowStateProcessor> processingInstances;
  private long startTimeSeconds;
  private Thread thread;

  WorkflowStateProcessor(int instanceId, ObjectStringMapper objectMapper, WorkflowDefinitionService workflowDefinitions,
      WorkflowInstanceService workflowInstances, WorkflowInstanceDao workflowInstanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env,
      Map<Integer, WorkflowStateProcessor> processingInstances, WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.objectMapper = objectMapper;
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.workflowInstanceDao = workflowInstanceDao;
    this.processingInstances = processingInstances;
    this.executorListeners = asList(executorListeners);
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    illegalStateChangeAction = env.getRequiredProperty("nflow.illegal.state.change.action");
    unknownWorkflowTypeRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.type.retry.delay.minutes", Integer.class);
    unknownWorkflowStateRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.state.retry.delay.minutes", Integer.class);
  }

  @Override
  public void run() {
    try {
      MDC.put(MDC_KEY, String.valueOf(instanceId));
      startTimeSeconds = currentTimeMillis() / 1000;
      thread = currentThread();
      processingInstances.put(instanceId, this);
      runImpl();
    } catch (Throwable ex) {
      logger.error("Unexpected failure occurred", ex);
    } finally {
      processingInstances.remove(instanceId);
      MDC.remove(MDC_KEY);
    }
  }

  private void runImpl() {
    logger.debug("Starting.");
    WorkflowInstance instance = workflowInstances.getWorkflowInstance(instanceId,
        EnumSet.of(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS, WorkflowInstanceInclude.CURRENT_STATE_VARIABLES,
            WorkflowInstanceInclude.STARTED),
        null);
    logIfLagging(instance);
    AbstractWorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    if (definition == null) {
      rescheduleUnknownWorkflowType(instance);
      return;
    }
    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.status == executing) {
      StateExecutionImpl execution = new StateExecutionImpl(instance, objectMapper, workflowInstanceDao,
          workflowInstancePreProcessor, workflowInstances);
      ListenerContext listenerContext = new ListenerContext(definition, instance, execution);
      WorkflowInstanceAction.Builder actionBuilder = new WorkflowInstanceAction.Builder(instance);
      WorkflowState state;
      try {
        state = definition.getState(instance.state);
      } catch (@SuppressWarnings("unused") IllegalStateException e) {
        rescheduleUnknownWorkflowState(instance);
        return;
      }

      try {
        processBeforeListeners(listenerContext);
        listenerContext.nextAction = processWithListeners(listenerContext, instance, definition, execution, state);
      } catch (Throwable t) {
        execution.setFailed(t);
        logger.error("Handler threw exception, trying again later.", t);
        execution.setRetry(true);
        execution.setNextState(state);
        execution.setNextStateReason(getStackTrace(t));
        handleRetry(execution, definition);
      } finally {
        if (execution.isFailed()) {
          processAfterFailureListeners(listenerContext, execution.getThrown());
        } else {
          processAfterListeners(listenerContext);
        }
        subsequentStateExecutions = busyLoopPrevention(state, settings, subsequentStateExecutions, execution);
        instance = saveWorkflowInstanceState(execution, instance, definition, actionBuilder);
      }
    }
    logger.debug("Finished.");
  }

  void logIfLagging(WorkflowInstance instance) {
    Duration executionLag = new Duration(instance.nextActivation, now());
    if (executionLag.isLongerThan(standardMinutes(1))) {
      laggingLogger.warn("Execution lagging {} seconds.", executionLag.getStandardSeconds());
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

  private int busyLoopPrevention(WorkflowState state, WorkflowSettings settings, int subsequentStateExecutions,
      StateExecutionImpl execution) {
    DateTime nextActivation = execution.getNextActivation();
    int maxSubsequentStateExecutions = settings.getMaxSubsequentStateExecutions(state);
    if (subsequentStateExecutions++ >= maxSubsequentStateExecutions && nextActivation != null) {
      logger.warn("Executed {} times without delay, forcing short transition delay", maxSubsequentStateExecutions);
      DateTime shortTransitionActivation = settings.getShortTransitionActivation();
      if (nextActivation.isBefore(shortTransitionActivation)) {
        execution.setNextActivation(shortTransitionActivation);
      }
    }
    return subsequentStateExecutions;
  }

  private WorkflowInstance saveWorkflowInstanceState(StateExecutionImpl execution, WorkflowInstance instance,
      AbstractWorkflowDefinition<?> definition, WorkflowInstanceAction.Builder actionBuilder) {
    if (definition.getMethod(execution.getNextState()) == null && execution.getNextActivation() != null) {
      logger.debug("No handler method defined for {}, clearing next activation", execution.getNextState());
      execution.setNextActivation(null);
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance) //
        .setNextActivation(execution.getNextActivation()) //
        .setStatus(getStatus(execution, definition.getState(execution.getNextState()))) //
        .setStateText(getStateText(instance, execution)) //
        .setState(execution.getNextState()) //
        .setRetries(execution.isRetry() ? execution.getRetries() + 1 : 0);
    if (execution.isStateProcessInvoked()) {
      actionBuilder.setExecutionEnd(now()).setType(getActionType(execution)).setStateText(execution.getNextStateReason());
      if (execution.isFailed()) {
        workflowInstanceDao.updateWorkflowInstanceAfterExecution(builder.build(), actionBuilder.build(),
            Collections.<WorkflowInstance> emptyList(), Collections.<WorkflowInstance> emptyList(), true);
      } else {
        workflowInstanceDao.updateWorkflowInstanceAfterExecution(builder.build(), actionBuilder.build(),
            execution.getNewChildWorkflows(), execution.getNewWorkflows(), execution.createAction());
        processSuccess(execution, instance);
      }
    } else {
      workflowInstanceDao.updateWorkflowInstance(builder.build());
    }
    return builder.setOriginalStateVariables(instance.stateVariables).build();
  }

  private void processSuccess(StateExecutionImpl execution, WorkflowInstance instance) {
    execution.getWakeUpParentWorkflowStates().ifPresent(expectedStates -> {
      logger.debug("Possibly waking up parent workflow instance {}", instance.parentWorkflowId);
      boolean notified = workflowInstanceDao.wakeUpWorkflowExternally(instance.parentWorkflowId, expectedStates);
      if (notified) {
        logger.info("Woke up parent workflow instance {}", instance.parentWorkflowId);
      } else {
        logger.info("Did not woke up parent workflow instance {}", instance.parentWorkflowId);
      }
    });
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
    return nextState.getType().getStatus(execution.getNextActivation());
  }

  private WorkflowActionType getActionType(StateExecutionImpl execution) {
    return execution.isFailed() || execution.isRetryCountExceeded() ? stateExecutionFailed : stateExecution;
  }

  private boolean isNextActivationImmediately(StateExecutionImpl execution) {
    return execution.isStateProcessInvoked() && execution.getNextActivation() != null
        && !execution.getNextActivation().isAfterNow();
  }

  private NextAction processWithListeners(ListenerContext listenerContext, WorkflowInstance instance,
      AbstractWorkflowDefinition<? extends WorkflowState> definition, StateExecutionImpl execution, WorkflowState state) {
    ProcessingExecutorListener processingListener = new ProcessingExecutorListener(instance, definition, execution, state);
    List<WorkflowExecutorListener> chain = new ArrayList<>(executorListeners.size() + 1);
    chain.addAll(executorListeners);
    chain.add(processingListener);
    NextAction nextAction = new ExecutorListenerChain(chain).next(listenerContext);
    if (execution.isStateProcessInvoked()) {
      return nextAction;
    }
    return new SkippedStateHandler(nextAction, instance, definition, execution, state).processState();
  }

  static class ExecutorListenerChain implements ListenerChain {
    private final Iterator<WorkflowExecutorListener> chain;

    ExecutorListenerChain(List<WorkflowExecutorListener> chain) {
      this.chain = chain.iterator();
    }

    @Override
    public NextAction next(ListenerContext context) {
      Assert.isTrue(chain.hasNext(), "Ran out of listeners in listener chain. The last listener must not call "
          + this.getClass().getSimpleName() + ".next().");
      return chain.next().process(context, this);
    }
  }

  private class ProcessingExecutorListener extends AbstractWorkflowExecutorListener {
    private final WorkflowInstance instance;
    private final AbstractWorkflowDefinition<? extends WorkflowState> definition;
    private final StateExecutionImpl execution;
    private final WorkflowState state;

    public ProcessingExecutorListener(WorkflowInstance instance, AbstractWorkflowDefinition<? extends WorkflowState> definition,
        StateExecutionImpl execution, WorkflowState state) {
      this.instance = instance;
      this.definition = definition;
      this.execution = execution;
      this.state = state;
    }

    @Override
    public NextAction process(ListenerContext listenerContext, ListenerChain chain) {
      return new NormalStateHandler(instance, definition, execution, state).processState();
    }
  }

  private class NormalStateHandler extends StateHandler {

    public NormalStateHandler(WorkflowInstance instance, AbstractWorkflowDefinition<?> definition, StateExecutionImpl execution,
        WorkflowState currentState) {
      super(instance, definition, execution, currentState);
    }

    @Override
    protected NextAction getNextAction(WorkflowStateMethod method, Object... args) {
      execution.setStateProcessInvoked(true);
      return (NextAction) invokeMethod(method.method, definition, args);
    }
  }

  private class SkippedStateHandler extends StateHandler {
    private final NextAction nextAction;

    public SkippedStateHandler(NextAction nextAction, WorkflowInstance instance, AbstractWorkflowDefinition<?> definition,
        StateExecutionImpl execution, WorkflowState currentState) {
      super(instance, definition, execution, currentState);
      this.nextAction = nextAction;
    }

    @Override
    protected NextAction getNextAction(WorkflowStateMethod method, Object... args) {
      return nextAction;
    }
  }

  private abstract class StateHandler {
    protected final WorkflowInstance instance;
    protected final AbstractWorkflowDefinition<?> definition;
    protected final StateExecutionImpl execution;
    protected final WorkflowState currentState;

    public StateHandler(WorkflowInstance instance, AbstractWorkflowDefinition<?> definition, StateExecutionImpl execution,
        WorkflowState currentState) {
      this.instance = instance;
      this.definition = definition;
      this.execution = execution;
      this.currentState = currentState;
    }

    protected abstract NextAction getNextAction(WorkflowStateMethod method, Object... args);

    public NextAction processState() {
      WorkflowStateMethod method = definition.getMethod(instance.state);
      if (method == null) {
        execution.setNextState(currentState);
        return stopInState(currentState, "Execution finished.");
      }
      NextAction nextAction;
      Object[] args = objectMapper.createArguments(execution, method);
      if (currentState.getType().isFinal()) {
        getNextAction(method, args);
        nextAction = stopInState(currentState, "Stopped in final state");
      } else {
        WorkflowState errorState = definition.getErrorState();
        try {
          nextAction = getNextAction(method, args);
          if (nextAction == null) {
            logger.error("State '{}' handler method returned null, proceeding to error state '{}'", instance.state, errorState);
            nextAction = moveToState(errorState, "State handler method returned null");
            execution.setFailed();
          } else {
            WorkflowState nextState = nextAction.getNextState();
            if (nextState != null && !definition.getStates().contains(nextState)) {
              logger.error("State '{}' is not a state of '{}' workflow definition, proceeding to error state '{}'", nextState,
                  definition.getType(), errorState);
              nextAction = moveToState(errorState,
                  "State '" + instance.state + "' handler method returned invalid next state '" + nextState + "'");
              execution.setFailed();
            } else if (!"ignore".equals(illegalStateChangeAction) && !definition.isAllowedNextAction(instance, nextAction)) {
              logger.warn("State transition from '{}' to '{}' is not allowed by workflow definition.", instance.state, nextState);
              if ("fail".equals(illegalStateChangeAction)) {
                nextAction = moveToState(errorState, "Illegal state transition from " + instance.state + " to " + nextState
                    + ", proceeding to error state " + errorState);
                execution.setFailed();
              }
            }
          }
        } catch (InvalidNextActionException e) {
          logger.error("State '" + instance.state
              + "' handler method failed to return valid next action, proceeding to error state '" + errorState + "'", e);
          nextAction = moveToState(errorState, e.getMessage());
          execution.setFailed(e);
        }
      }
      execution.setNextActivation(nextAction.getActivation());
      execution.setNextStateReason(nextAction.getReason());

      if (!execution.isStateProcessInvoked()) {
        execution.setNextState(currentState);
      } else if (nextAction.isRetry()) {
        execution.setNextState(currentState);
        execution.setRetry(true);
        handleRetryAfter(execution, nextAction.getActivation(), definition);
      } else {
        execution.setNextState(nextAction.getNextState());
      }
      objectMapper.storeArguments(execution, method, args);
      return nextAction;
    }

  }

  /**
   * Handle retries for the state execution. Moves the workflow to a failure state after the maximum retry attempts is exceeded.
   * If there is no failure state defined for the retried state, moves the workflow to the generic error state and stops
   * processing. Error state handler method, if it exists, is not executed. If the maximum retry attempts is not exceeded,
   * schedules the next attempt for the state based on workflow settings. This method is called when an unexpected exception
   * happens during state method handling.
   *
   * @param execution
   *          State execution information.
   * @param definition
   *          Workflow definition
   */
  void handleRetry(StateExecutionImpl execution, AbstractWorkflowDefinition<?> definition) {
    handleRetryAfter(execution, definition.getSettings().getErrorTransitionActivation(execution.getRetries()), definition);
  }

  /**
   * Handle retries for the state execution. Moves the workflow to a failure state after the maximum retry attempts is exceeded.
   * If there is no failure state defined for the retried state, moves the workflow to the generic error state and stops
   * processing. Error state handler method, if it exists, is not executed. If the maximum retry attempts is not exceeded,
   * schedules the next attempt to the given activation time. This method is called when a retry attempt is explicitly requested
   * by a state handling method.
   *
   * @param execution
   *          State execution information.
   * @param activation
   *          Time for next retry attempt.
   * @param definition
   *          Workflow definition
   */
  void handleRetryAfter(StateExecutionImpl execution, DateTime activation, AbstractWorkflowDefinition<?> definition) {
    if (execution.getRetries() >= definition.getSettings().maxRetries) {
      execution.setRetry(false);
      execution.setRetryCountExceeded();
      String currentStateName = execution.getCurrentStateName();
      WorkflowState failureState = definition.getFailureTransitions().get(currentStateName);
      WorkflowState currentState = definition.getState(currentStateName);
      if (failureState != null) {
        execution.setNextState(failureState);
        execution.setNextStateReason("Max retry count exceeded, going to failure state");
        execution.setNextActivation(now());
      } else {
        WorkflowState errorState = definition.getErrorState();
        execution.setNextState(errorState);
        if (errorState.equals(currentState)) {
          execution.setNextStateReason("Max retry count exceeded when handling error state, processing stopped");
          execution.setNextActivation(null);
        } else {
          execution.setNextStateReason("Max retry count exceeded, no failure state defined, going to error state");
          execution.setNextActivation(now());
        }
      }
    } else {
      execution.setNextActivation(activation);
    }
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

  public long getStartTimeSeconds() {
    return startTimeSeconds;
  }

  public void logPotentiallyStuck(long processingTimeSeconds) {
    threadStuckLogger.warn("Workflow instance {} has been processed for {} seconds, it may be stuck.\n{}", instanceId,
        processingTimeSeconds, getStackTraceAsString());
  }

  private StringBuilder getStackTraceAsString() {
    StringBuilder sb = new StringBuilder(2000);
    for (StackTraceElement element : thread.getStackTrace()) {
      sb.append(element).append('\n');
    }
    return sb;
  }
}
