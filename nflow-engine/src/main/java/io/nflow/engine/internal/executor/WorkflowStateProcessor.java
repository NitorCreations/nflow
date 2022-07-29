package io.nflow.engine.internal.executor;

import static io.nflow.engine.service.WorkflowInstanceInclude.CURRENT_STATE_VARIABLES;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.joda.time.DateTime.now;
import static org.joda.time.Duration.standardMinutes;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.nflow.engine.internal.workflow.StoredWorkflowDefinitionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.util.Assert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.exception.StateProcessExceptionHandling;
import io.nflow.engine.exception.StateSaveExceptionAnalyzer;
import io.nflow.engine.exception.StateSaveExceptionHandling;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.NflowLogger;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.StateExecutionImpl;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.internal.workflow.WorkflowStateMethod;
import io.nflow.engine.listener.ListenerChain;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

class WorkflowStateProcessor implements Runnable {

  @SuppressFBWarnings(value = "LO_NON_PRIVATE_STATIC_LOGGER", justification = "Used by inner class")
  private static final Logger logger = getLogger(WorkflowStateProcessor.class);
  private static final PeriodicLogger laggingLogger = new PeriodicLogger(logger, 30);
  private static final PeriodicLogger threadStuckLogger = new PeriodicLogger(logger, 60);
  private static final String MDC_KEY = "workflowInstanceId";

  final long instanceId;
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final Supplier<Boolean> shutdownRequested;
  final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final MaintenanceDao maintenanceDao;
  private final List<WorkflowExecutorListener> executorListeners;
  final String illegalStateChangeAction;
  private final int unknownWorkflowTypeRetryDelay;
  private final int unknownWorkflowStateRetryDelay;
  private final int stateProcessingRetryDelay;
  private final int stateVariableValueTooLongRetryDelay;
  private final Map<Long, WorkflowStateProcessor> processingInstances;
  private final NflowLogger nflowLogger;
  private final StateSaveExceptionAnalyzer stateSaveExceptionAnalyzer;
  private DateTime startTime;
  private Thread thread;
  private ListenerContext listenerContext;

  WorkflowStateProcessor(long instanceId, Supplier<Boolean> shutdownRequested, ObjectStringMapper objectMapper,
      WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      WorkflowInstanceDao workflowInstanceDao, MaintenanceDao maintenanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env,
      Map<Long, WorkflowStateProcessor> processingInstances, NflowLogger nflowLogger,
      StateSaveExceptionAnalyzer stateSaveExceptionAnalyzer, WorkflowExecutorListener... executorListeners) {
    this.instanceId = instanceId;
    this.shutdownRequested = shutdownRequested;
    this.objectMapper = objectMapper;
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.workflowInstanceDao = workflowInstanceDao;
    this.maintenanceDao = maintenanceDao;
    this.processingInstances = processingInstances;
    this.nflowLogger = nflowLogger;
    this.stateSaveExceptionAnalyzer = stateSaveExceptionAnalyzer;
    this.executorListeners = asList(executorListeners);
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    illegalStateChangeAction = env.getRequiredProperty("nflow.illegal.state.change.action");
    unknownWorkflowTypeRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.type.retry.delay.minutes", Integer.class);
    unknownWorkflowStateRetryDelay = env.getRequiredProperty("nflow.unknown.workflow.state.retry.delay.minutes", Integer.class);
    stateProcessingRetryDelay = env.getRequiredProperty("nflow.executor.stateProcessingRetryDelay.seconds", Integer.class);
    stateVariableValueTooLongRetryDelay = env.getRequiredProperty("nflow.executor.stateVariableValueTooLongRetryDelay.minutes",
        Integer.class);
  }

  @Override
  public void run() {
    MDC.put(MDC_KEY, String.valueOf(instanceId));
    startTime = now();
    thread = currentThread();
    processingInstances.put(instanceId, this);
    while (true) {
      try {
        runImpl();
        break;
      } catch (Throwable ex) {
        if (shutdownRequested.get()) {
          logger.error("Failed to process workflow instance and shutdown requested", ex);
          break;
        }
        logger.error("Failed to process workflow instance {}, retrying after {} seconds", instanceId, stateProcessingRetryDelay,
            ex);
        sleepIgnoreInterrupted(stateProcessingRetryDelay);
      }
    }
    processingInstances.remove(instanceId);
    MDC.remove(MDC_KEY);
  }

  private void runImpl() {
    logger.debug("Starting.");
    WorkflowInstance instance = workflowInstances.getWorkflowInstance(instanceId, EnumSet.of(CURRENT_STATE_VARIABLES), null);
    logIfLagging(instance);
    WorkflowDefinition definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    if (definition == null || definition instanceof StoredWorkflowDefinitionWrapper) {
      rescheduleUnknownWorkflowType(instance);
      return;
    }
    WorkflowSettings settings = definition.getSettings();
    int subsequentStateExecutions = 0;
    while (instance.status == executing && !shutdownRequested.get()) {
      startTime = now();
      StateExecutionImpl execution = new StateExecutionImpl(instance, objectMapper, workflowInstanceDao,
          workflowInstancePreProcessor, workflowInstances);
      listenerContext = new ListenerContext(definition, instance, execution);
      WorkflowInstanceAction.Builder actionBuilder = new WorkflowInstanceAction.Builder(instance);
      WorkflowState state;
      try {
        state = definition.getState(instance.state);
      } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
        rescheduleUnknownWorkflowState(instance);
        return;
      }
      boolean saveInstanceState = true;
      try {
        processBeforeListeners();
        listenerContext.nextAction = processWithListeners(instance, definition, execution, state);
      } catch (StateVariableValueTooLongException e) {
        instance = rescheduleStateVariableValueTooLong(e, instance);
        saveInstanceState = false;
      } catch (Throwable thrown) {
        if (thrown instanceof UndeclaredThrowableException) {
          thrown = thrown.getCause();
        }
        execution.setFailed(thrown);
        StateProcessExceptionHandling exceptionHandling = settings.analyzeExeption(state, thrown);
        if (exceptionHandling.isRetryable) {
          logRetryableException(exceptionHandling, state.name(), thrown);
          execution.setRetry(true);
          execution.setNextState(state);
          var reason = getStackTrace(thrown);
          var retryAfter = definition.getSettings().getErrorTransitionActivation(execution.getRetries());
          if (shutdownRequested.get()) {
            if (thrown instanceof InterruptedException) {
              reason = "Shutdown: " + reason;
              // if we're shutting down there is no reason to delay the next attempt at much later time
              retryAfter = now();
            }
          }
          execution.setNextStateReason(reason);
          execution.handleRetryAfter(retryAfter, definition);
        } else {
          logger.error("Handler threw an exception and retrying is not allowed, going to failure state.", thrown);
          execution.handleFailure(definition, "Handler threw an exception and retrying is not allowed");
        }
      } finally {
        if (saveInstanceState) {
          if (execution.isFailed()) {
            processAfterFailureListeners(execution.getThrown());
          } else {
            processAfterListeners();
            optionallyCleanupWorkflowInstanceHistory(definition.getSettings(), execution);
          }
          subsequentStateExecutions = busyLoopPrevention(state, settings, subsequentStateExecutions, execution);
          instance = saveWorkflowInstanceState(execution, instance, definition, actionBuilder);
        }
      }
    }
    logger.debug("Finished.");
  }

  private void logRetryableException(StateProcessExceptionHandling exceptionHandling, String state, Throwable thrown) {
    if (exceptionHandling.logStackTrace) {
      nflowLogger.log(logger, exceptionHandling.logLevel, "Handling state '{}' threw a retryable exception, trying again later.",
              state, thrown);
    } else {
      nflowLogger.log(logger, exceptionHandling.logLevel,
          "Handling state '{}' threw a retryable exception, trying again later. Message: {}",
              state, thrown.getMessage());
    }
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

  private WorkflowInstance rescheduleStateVariableValueTooLong(StateVariableValueTooLongException e, WorkflowInstance instance) {
    logger.warn("Failed to process workflow instance {}: {} - rescheduling workflow instance", instance.id, e.getMessage());
    instance = new WorkflowInstance.Builder(instance).setNextActivation(now().plusMinutes(stateVariableValueTooLongRetryDelay))
        .setStatus(inProgress).setStateText(e.getMessage()).build();
    workflowInstanceDao.updateWorkflowInstance(instance);
    return instance;
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
      WorkflowDefinition definition, WorkflowInstanceAction.Builder actionBuilder) {
    if (definition.getMethod(execution.getNextState()) == null && execution.getNextActivation() != null) {
      logger.debug("No handler method defined for {}, clearing next activation", execution.getNextState().name());
      execution.setNextActivation(null);
    }
    WorkflowState nextState = definition.getState(execution.getNextState().name());
    if (instance.parentWorkflowId != null && nextState.getType() == WorkflowStateType.end) {
      try {
        String parentType = workflowInstanceDao.getWorkflowInstanceType(instance.parentWorkflowId);
        WorkflowDefinition parentDefinition = workflowDefinitions.getWorkflowDefinition(parentType);
        String[] waitStates = parentDefinition.getStates().stream()
            .filter(state -> state.getType() == WorkflowStateType.wait)
            .map(WorkflowState::name)
            .toArray(String[]::new);
        if (waitStates.length > 0) {
          execution.wakeUpParentWorkflow(waitStates);
        }
      } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
        // parent has been archived or deleted, no need to wake it up anymore
      }
    }
    WorkflowInstance.Builder instanceBuilder = new WorkflowInstance.Builder(instance)
        .setNextActivation(execution.getNextActivation())
        .setStatus(getStatus(execution, nextState))
        .setStateText(getStateText(instance, execution))
        .setState(execution.getNextState())
        .setRetries(execution.isRetry() ? execution.getRetries() + 1 : 0);
    int saveRetryCount = 0;
    if (execution.getNewBusinessKey() != null) {
      instanceBuilder.setBusinessKey(execution.getNewBusinessKey());
    }
    while (true) {
      try {
        return persistWorkflowInstanceState(execution, instance.stateVariables, actionBuilder, instanceBuilder);
      } catch (Exception ex) {
        if (shutdownRequested.get()) {
          logger.error(
              "Failed to save workflow instance {} new state, not retrying due to shutdown request. The state will be rerun on recovery.",
              instance.id, ex);
          // return the original instance since persisting failed
          return instance;
        }
        StateSaveExceptionHandling handling = stateSaveExceptionAnalyzer.analyzeSafely(ex, saveRetryCount++);
        if (handling.logStackTrace) {
          nflowLogger.log(logger, handling.logLevel, "Failed to save workflow instance {} new state, retrying after {} seconds.",
                  instance.id, handling.retryDelay, ex);
        } else {
          nflowLogger.log(logger, handling.logLevel,
              "Failed to save workflow instance {} new state, retrying after {} seconds. Error: {}",
                  instance.id, handling.retryDelay, ex.getMessage());
        }
        sleepIgnoreInterrupted(handling.retryDelay.getStandardSeconds());
      }
    }
  }

  private WorkflowInstance persistWorkflowInstanceState(StateExecutionImpl execution, Map<String, String> originalStateVars,
      WorkflowInstanceAction.Builder actionBuilder, WorkflowInstance.Builder instanceBuilder) {
    if (execution.isStateProcessInvoked()) {
      WorkflowInstanceAction action = actionBuilder.setExecutionEnd(now()).setType(getActionType(execution))
          .setStateText(execution.getNextStateReason()).build();
      WorkflowInstance instance = instanceBuilder.setStartedIfNotSet(action.executionStart).build();
      if (execution.isFailed()) {
        workflowInstanceDao.updateWorkflowInstanceAfterExecution(instance, action, emptyList(), emptyList(), true);
      } else {
        workflowInstanceDao.updateWorkflowInstanceAfterExecution(instance, action, execution.getNewChildWorkflows(),
            execution.getNewWorkflows(), execution.createAction());
        processSuccess(execution, instance);
      }
    } else {
      workflowInstanceDao.updateWorkflowInstance(instanceBuilder.build());
    }
    return instanceBuilder.setOriginalStateVariables(originalStateVars).build();
  }

  private void processSuccess(StateExecutionImpl execution, WorkflowInstance instance) {
    execution.getWakeUpParentWorkflowStates().ifPresent(expectedStates -> {
      logger.debug("Possibly waking up parent workflow instance {}", instance.parentWorkflowId);
      try {
        boolean notified = workflowInstanceDao.wakeUpWorkflowExternally(instance.parentWorkflowId, expectedStates);
        if (notified) {
          logger.info("Woke up parent workflow instance {}", instance.parentWorkflowId);
        } else {
          logger.info("Did not woke up parent workflow instance {}", instance.parentWorkflowId);
        }
      } catch (DataAccessException e) {
        logger.error("Did not woke up parent workflow instance {}", instance.parentWorkflowId, e);
      }
    });
  }

  private String getStateText(WorkflowInstance instance, StateExecutionImpl execution) {
    if (execution.isRetry() || execution.isRetryCountExceeded()) {
      return execution.getNextStateReason();
    }
    if (execution.getNextActivation() == null) {
      return "Stopped in state " + execution.getNextState().name();
    }
    return "Scheduled by previous state " + instance.state;
  }

  private WorkflowInstanceStatus getStatus(StateExecutionImpl execution, WorkflowState nextState) {
    if (!shutdownRequested.get() && isNextActivationImmediately(execution)) {
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

  private NextAction processWithListeners(WorkflowInstance instance, WorkflowDefinition definition, StateExecutionImpl execution,
      WorkflowState state) {
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

  private void optionallyCleanupWorkflowInstanceHistory(WorkflowSettings settings, StateExecutionImpl execution) {
    try {
      if (settings.historyDeletableAfter != null && !shutdownRequested.get()
          && (execution.isHistoryCleaningForced() || settings.deleteWorkflowInstanceHistory())) {
        DateTime olderThan = DateTime.now().minus(settings.historyDeletableAfter);
        logger.debug("Cleaning workflow instance {} history older than {}", instanceId, olderThan);
        maintenanceDao.deleteActionAndStateHistory(instanceId, olderThan);
      }
    } catch (Throwable t) {
      logger.error("Failure in workflow instance {} history cleanup", instanceId, t);
    }
  }

  private void sleepIgnoreInterrupted(long seconds) {
    try {
      SECONDS.sleep(seconds);
    } catch (@SuppressWarnings("unused") InterruptedException ok) {
    }
  }

  static class ExecutorListenerChain implements ListenerChain {
    private final Iterator<WorkflowExecutorListener> chain;

    ExecutorListenerChain(Collection<WorkflowExecutorListener> chain) {
      this.chain = chain.iterator();
    }

    @Override
    public NextAction next(ListenerContext context) {
      Assert.isTrue(chain.hasNext(), "Ran out of listeners in listener chain. The last listener must not call "
          + this.getClass().getSimpleName() + ".next().");
      return chain.next().process(context, this);
    }
  }

  private class ProcessingExecutorListener implements WorkflowExecutorListener {
    private final WorkflowInstance instance;
    private final WorkflowDefinition definition;
    private final StateExecutionImpl execution;
    private final WorkflowState state;

    public ProcessingExecutorListener(WorkflowInstance instance, WorkflowDefinition definition, StateExecutionImpl execution,
        WorkflowState state) {
      this.instance = instance;
      this.definition = definition;
      this.execution = execution;
      this.state = state;
    }

    @Override
    public NextAction process(ListenerContext context, ListenerChain chain) {
      return new NormalStateHandler(instance, definition, execution, state).processState();
    }
  }

  private class NormalStateHandler extends StateHandler {

    public NormalStateHandler(WorkflowInstance instance, WorkflowDefinition definition, StateExecutionImpl execution,
        WorkflowState currentState) {
      super(instance, definition, execution, currentState);
    }

    @Override
    protected NextAction processStepToGetNextAction(WorkflowStateMethod method, Object... args) {
      execution.setStateProcessInvoked(true);
      return (NextAction) invokeMethod(method.method, definition, args);
    }
  }

  private class SkippedStateHandler extends StateHandler {
    private final NextAction nextAction;

    public SkippedStateHandler(NextAction nextAction, WorkflowInstance instance, WorkflowDefinition definition,
        StateExecutionImpl execution, WorkflowState currentState) {
      super(instance, definition, execution, currentState);
      this.nextAction = nextAction;
    }

    @Override
    protected NextAction processStepToGetNextAction(WorkflowStateMethod method, Object... args) {
      return nextAction;
    }
  }

  private abstract class StateHandler {
    protected final WorkflowInstance instance;
    protected final WorkflowDefinition definition;
    protected final StateExecutionImpl execution;
    protected final WorkflowState currentState;

    public StateHandler(WorkflowInstance instance, WorkflowDefinition definition, StateExecutionImpl execution,
        WorkflowState currentState) {
      this.instance = instance;
      this.definition = definition;
      this.execution = execution;
      this.currentState = currentState;
    }

    protected abstract NextAction processStepToGetNextAction(WorkflowStateMethod method, Object... args);

    public NextAction processState() {
      WorkflowStateMethod method = definition.getMethod(instance.state);
      if (method == null) {
        execution.setNextState(currentState);
        return stopInState(currentState, "Execution finished.");
      }
      NextAction nextAction;
      Object[] args = objectMapper.createArguments(execution, method);
      if (currentState.getType().isFinal()) {
        processStepToGetNextAction(method, args);
        nextAction = stopInState(currentState, "Stopped in final state");
      } else {
        WorkflowState errorState = definition.getErrorState();
        try {
          nextAction = processStepToGetNextAction(method, args);
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
          logger.error("State '{}' handler method failed to return valid next action, proceeding to error state '{}'",
              instance.state, errorState, e);
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
        execution.handleRetryAfter(nextAction.getActivation(), definition);
      } else {
        execution.setNextState(nextAction.getNextState());
      }
      objectMapper.storeArguments(execution, method, args);
      return nextAction;
    }

  }

  private void processBeforeListeners() {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.beforeProcessing(listenerContext);
      } catch (Throwable t) {
        logger.error("Error in {}.beforeProcessing ({})", listener.getClass().getName(), t.getMessage(), t);
      }
    }
  }

  private void processAfterListeners() {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.afterProcessing(listenerContext);
      } catch (Throwable t) {
        logger.error("Error in {}.afterProcessing ({})", listener.getClass().getName(), t.getMessage(), t);
      }
    }
  }

  private void processAfterFailureListeners(Throwable ex) {
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        listener.afterFailure(listenerContext, ex);
      } catch (Throwable t) {
        logger.error("Error in {}.afterFailure ({})", listener.getClass().getName(), t.getMessage(), t);
      }
    }
  }

  public DateTime getStartTime() {
    return startTime;
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

  public void handlePotentiallyStuck(Duration processingTime) {
    boolean interrupt = false;
    for (WorkflowExecutorListener listener : executorListeners) {
      try {
        if (listener.handlePotentiallyStuck(listenerContext, processingTime)) {
          interrupt = true;
        }
      } catch (Throwable t) {
        logger.error("Error in " + listener.getClass().getName() + ".handleStuck (" + t.getMessage() + ")", t);
      }
    }
    if (interrupt) {
      thread.interrupt();
    }
  }
}
