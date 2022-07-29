package io.nflow.engine.internal.executor;

import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.AlwaysCleanTestWorkflow.ALWAYS_CLEAN_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.ExecuteTestWorkflow.EXECUTE_TEST_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.FailCleaningTestWorkflow.FAIL_CLEANING_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.FailingTestWorkflow.FAILING_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.LoopingTestWorkflow.LOOPING_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.NeverCleanTestWorkflow.NEVER_CLEAN_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.NonRetryableTestWorkflow.NON_RETRYABLE_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.NotifyTestWorkflow.NOTIFY_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.NotifyTestWorkflow.WAKE_PARENT;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.SimpleTestWorkflow.SIMPLE_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.StateVariableTestWorkflow.STATE_VARIABLE_TYPE;
import static io.nflow.engine.internal.executor.WorkflowStateProcessorTest.StuckTestWorkflow.STUCK_TYPE;
import static io.nflow.engine.service.WorkflowInstanceInclude.CURRENT_STATE_VARIABLES;
import static io.nflow.engine.workflow.curated.BulkWorkflow.BULK_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.curated.BulkWorkflow.WAIT_FOR_CHILDREN_TO_FINISH;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.TestDefinition.START_1;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.manual;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static java.lang.Boolean.FALSE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.joda.time.Duration.standardHours;
import static org.joda.time.Period.hours;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import io.nflow.engine.internal.workflow.StoredWorkflowDefinitionWrapper;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.exception.StateProcessExceptionHandling;
import io.nflow.engine.exception.StateSaveExceptionAnalyzer;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.NflowLogger;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.StateExecutionImpl;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.listener.ListenerChain;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.curated.BulkWorkflow;
import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.TestDefinition;
import io.nflow.engine.workflow.definition.TestState;
import io.nflow.engine.workflow.definition.TestWorkflow;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

public class WorkflowStateProcessorTest extends BaseNflowTest {

  @Mock
  WorkflowDefinitionService workflowDefinitions;

  @Mock
  WorkflowInstanceService workflowInstances;

  @Mock
  WorkflowInstanceDao workflowInstanceDao;

  @Mock
  MaintenanceDao maintenanceDao;

  MockEnvironment env = new MockEnvironment();

  @Mock
  WorkflowExecutorListener listener1;

  @Mock
  WorkflowExecutorListener listener2;

  @Mock
  WorkflowInstancePreProcessor workflowInstancePreProcessor;

  final NflowLogger nflowLogger = new NflowLogger();

  @Mock
  StateSaveExceptionAnalyzer stateSaveExceptionAnalyzer;

  @Mock
  StateExecutionImpl executionMock;

  @Captor
  ArgumentCaptor<WorkflowInstance> update;

  @Captor
  ArgumentCaptor<WorkflowInstanceAction> action;

  @Captor
  ArgumentCaptor<List<WorkflowInstance>> childWorkflows;

  @Captor
  ArgumentCaptor<List<WorkflowInstance>> workflows;

  ObjectStringMapper objectMapper = new ObjectStringMapper(new ObjectMapper());

  WorkflowStateProcessor executor;

  ExecuteTestWorkflow executeWf = new ExecuteTestWorkflow();

  NeverCleanTestWorkflow neverCleanWf = new NeverCleanTestWorkflow();

  AlwaysCleanTestWorkflow alwaysCleanWf = new AlwaysCleanTestWorkflow();

  FailCleaningTestWorkflow failCleaningWf = new FailCleaningTestWorkflow();

  SimpleTestWorkflow simpleWf = new SimpleTestWorkflow();

  FailingTestWorkflow failingWf = new FailingTestWorkflow();

  NotifyTestWorkflow notifyWf = new NotifyTestWorkflow();

  StateVariableTestWorkflow stateVariableWf = new StateVariableTestWorkflow();

  NonRetryableTestWorkflow nonRetryableWf = new NonRetryableTestWorkflow();

  LoopingTestWorkflow loopingWf = new LoopingTestWorkflow();

  StuckTestWorkflow stuckWf = new StuckTestWorkflow();

  static WorkflowInstance newChildWorkflow = mock(WorkflowInstance.class);

  static WorkflowInstance newWorkflow = mock(WorkflowInstance.class);

  static Map<Long, WorkflowStateProcessor> processingInstances;

  private final TestWorkflow testWorkflowDef = new TestWorkflow();

  private final Set<WorkflowInstanceInclude> INCLUDES = EnumSet.of(CURRENT_STATE_VARIABLES);

  private final AtomicBoolean shutdownRequest = new AtomicBoolean();

  @BeforeEach
  public void setup() {
    processingInstances = new ConcurrentHashMap<>();
    env.setProperty("nflow.illegal.state.change.action", "fail");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    env.setProperty("nflow.executor.stateProcessingRetryDelay.seconds", "1");
    env.setProperty("nflow.executor.stateSaveRetryDelay.seconds", "1");
    env.setProperty("nflow.executor.stateVariableValueTooLongRetryDelay.minutes", "60");
    env.setProperty("nflow.db.workflowInstanceType.cacheSize", "10000");
    executor = new WorkflowStateProcessor(1, shutdownRequest::get, objectMapper, workflowDefinitions, workflowInstances,
        workflowInstanceDao, maintenanceDao, workflowInstancePreProcessor, env, processingInstances, nflowLogger,
        stateSaveExceptionAnalyzer, listener1, listener2);
    setCurrentMillisFixed(currentTimeMillis());
    lenient().doReturn(executeWf).when(workflowDefinitions).getWorkflowDefinition(EXECUTE_TEST_TYPE);
    lenient().doReturn(neverCleanWf).when(workflowDefinitions).getWorkflowDefinition(NEVER_CLEAN_TYPE);
    lenient().doReturn(alwaysCleanWf).when(workflowDefinitions).getWorkflowDefinition(ALWAYS_CLEAN_TYPE);
    lenient().doReturn(failCleaningWf).when(workflowDefinitions).getWorkflowDefinition(FAIL_CLEANING_TYPE);
    lenient().doReturn(simpleWf).when(workflowDefinitions).getWorkflowDefinition(SIMPLE_TYPE);
    lenient().doReturn(failingWf).when(workflowDefinitions).getWorkflowDefinition(FAILING_TYPE);
    lenient().doReturn(notifyWf).when(workflowDefinitions).getWorkflowDefinition(NOTIFY_TYPE);
    lenient().doReturn(stateVariableWf).when(workflowDefinitions).getWorkflowDefinition(STATE_VARIABLE_TYPE);
    lenient().doReturn(nonRetryableWf).when(workflowDefinitions).getWorkflowDefinition(NON_RETRYABLE_TYPE);
    lenient().doReturn(loopingWf).when(workflowDefinitions).getWorkflowDefinition(LOOPING_TYPE);
    lenient().doReturn(stuckWf).when(workflowDefinitions).getWorkflowDefinition(STUCK_TYPE);
    filterChain(listener1);
    filterChain(listener2);
    lenient().when(executionMock.getRetries()).thenReturn(testWorkflowDef.getSettings().maxRetries);
  }

  @AfterEach
  public void cleanUp() {
    setCurrentMillisSystem();
  }

  @Test
  public void runWorkflowThroughOneSuccessfulState() {
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstancePreProcessor.process(newChildWorkflow)).thenReturn(newChildWorkflow);
    when(workflowInstancePreProcessor.process(newWorkflow)).thenReturn(newWorkflow);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(inProgress, TestState.PROCESS, 0, is("Scheduled by previous state begin"))),
        argThat(matchesWorkflowInstanceAction(TestState.BEGIN, is("Process after delay"), 0, stateExecution)),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(childWorkflows.getValue().get(0), is(newChildWorkflow));
    assertThat(workflows.getValue().get(0), is(newWorkflow));
  }

  @Test
  public void runWorkflowThroughOneFailedState() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 1, containsString("test-fail"))),
        argThat(matchesWorkflowInstanceAction(TestState.BEGIN, containsString("test-fail"), 0, stateExecutionFailed)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void runWorkflowThroughToFailureState() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(TestState.BEGIN)
        .setRetries(failingWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.ERROR, 0, is("Max retry count exceeded, going to failure state")));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(TestState.BEGIN,
        is("Max retry count exceeded, going to failure state"), failingWf.getSettings().maxRetries, stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(manual, TestState.ERROR, 0, is("Stopped in state error"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void workflowStatusIsSetToExecutingWhenNextStateIsProcessedImmediately() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), anyBoolean());
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.PROCESS, 0, is("Scheduled by previous state begin")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(TestState.BEGIN, is("Move to processing."), 0, stateExecution));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, TestState.DONE, 0, is("Stopped in state done"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.PROCESS, is("Finished."), 0, stateExecution));
  }

  @Test
  public void actionIsNotCreatedWhenCreateActionIsSetToFalse() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(TestState.PROCESS).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(), childWorkflows.capture(),
        workflows.capture(), eq(false));
  }

  @Test
  public void workflowStatusIsSetToFinishedForFinalStates() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), anyBoolean());
    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, TestState.DONE, 0, is("Stopped in state done"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.PROCESS, is("Finished."), 0, stateExecution));
  }

  @Test
  public void instanceWithUnsupportedStateIsRescheduled() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState("invalid").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    DateTime oneHourInFuture = now().plusHours(1);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstance(argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.INVALID, 0,
        is("Unsupported workflow state"), greaterThanOrEqualTo(oneHourInFuture), is(nullValue()))));
  }

  @Test
  public void stateIsRescheduledWhenStateVariableValueIsTooLong() {
    WorkflowInstance instance = executingInstanceBuilder().setType(STATE_VARIABLE_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    String errorMessage = "Too long state variable value";
    doThrow(new StateVariableValueTooLongException(errorMessage)).when(workflowInstanceDao)
        .checkStateVariableValueLength(anyString(), anyString());
    DateTime oneHourInFuture = now().plusHours(1);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstance(argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 0,
        is(errorMessage), greaterThanOrEqualTo(oneHourInFuture), is(nullValue()))));
  }

  @Test
  public void workflowStatusIsSetToManualForManualStates() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(SimpleTestWorkflow.BEFORE_MANUAL)
        .setRetries(simpleWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(manual, SimpleTestWorkflow.MANUAL, 0, is("Stopped in state manualState"),
            nullValue(DateTime.class))),
        argThat(matchesWorkflowInstanceAction(SimpleTestWorkflow.BEFORE_MANUAL, is("Move to manual state."),
            simpleWf.getSettings().maxRetries, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void maxRetryIsObeyedForManualRetry() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE)
        .setState(FailingTestWorkflow.RETRYING_STATE).setRetries(failingWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0), matchesWorkflowInstance(executing, TestState.ERROR, 0,
        is("Max retry count exceeded, no failure state defined, going to error state")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(FailingTestWorkflow.RETRYING_STATE,
            is("Max retry count exceeded, no failure state defined, going to error state"), failingWf.getSettings().maxRetries,
            stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(manual, TestState.ERROR, 0, is("Stopped in state error"), is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void skippingWorkflowWithListenerCausesProcessorToStopProcessingWorkflow() {
    DateTime now = now();
    final DateTime skipped = now.plusHours(1);
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setNextActivation(now)
        .setState(TestState.BEGIN).setStateText("myStateText").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    WorkflowExecutorListener listener = mock(WorkflowExecutorListener.class);
    executor = new WorkflowStateProcessor(1, shutdownRequest::get, objectMapper, workflowDefinitions, workflowInstances,
        workflowInstanceDao, maintenanceDao, workflowInstancePreProcessor, env, processingInstances, nflowLogger,
        stateSaveExceptionAnalyzer, listener);

    doAnswer((Answer<NextAction>) invocation -> retryAfter(skipped, "")).when(listener).process(any(ListenerContext.class),
        any(ListenerChain.class));

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstance(argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 0,
        is("Scheduled by previous state begin"), is(skipped), is(nullValue()))));
  }

  @Test
  public void goToErrorStateWhenStateMethodReturnsNull() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE)
        .setState(FailingTestWorkflow.PROCESS_RETURN_NULL).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    filterChain(listener1);
    runExecutorWithTimeout();
    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1, times(2)).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), isNull());
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.ERROR, 0, is("Scheduled by previous state processReturnNull")));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.PROCESS_RETURN_NULL,
        is("State handler method returned null"), 0, stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(manual, TestState.ERROR, 0, is("Stopped in state error"), is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  private void filterChain(WorkflowExecutorListener listener) {
    lenient().doAnswer(invocation -> {
      ListenerContext context = (ListenerContext) invocation.getArguments()[0];
      ListenerChain chain = (ListenerChain) invocation.getArguments()[1];
      chain.next(context);
      return null;
    }).when(listener).process(any(ListenerContext.class), any(ListenerChain.class));
  }

  @Test
  public void goToErrorStateWhenNextStateIsNull() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE)
        .setState(FailingTestWorkflow.PROCESS_RETURN_NULL_NEXT_STATE).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1, times(2)).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0), matchesWorkflowInstance(executing, TestState.ERROR, 0,
        is("Scheduled by previous state processReturnNullNextState")));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.PROCESS_RETURN_NULL_NEXT_STATE,
        is("Next state can not be null"), 0, stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(manual, TestState.ERROR, 0, is("Stopped in state error"), is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void doNothingWhenNotifyingParentWithoutParentWorkflowId() {
    WorkflowInstance instance = executingInstanceBuilder().setType(NOTIFY_TYPE).setState(WAKE_PARENT).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, never()).wakeUpWorkflowExternally(any(Long.class), any(List.class));
  }

  @Test
  public void whenWakingUpParentWorkflowSucceeds() {
    WorkflowInstance instance = executingInstanceBuilder().setParentWorkflowId(999L).setType(NOTIFY_TYPE).setState(WAKE_PARENT)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstanceDao.getWorkflowInstanceType(instance.parentWorkflowId)).thenReturn("parentType");
    TestDefinition parentDefinition = mock(TestDefinition.class);
    doReturn(parentDefinition).when(workflowDefinitions).getWorkflowDefinition("parentType");
    when(parentDefinition.getStates()).thenReturn(new TestDefinition("parentType", START_1).getStates());
    when(workflowInstanceDao.wakeUpWorkflowExternally(999, emptyList())).thenReturn(true);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).wakeUpWorkflowExternally(999, emptyList());
  }

  @Test
  public void whenWakingUpParentWorkflowFails() {
    WorkflowInstance instance = executingInstanceBuilder().setParentWorkflowId(999L).setType(NOTIFY_TYPE).setState(WAKE_PARENT)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstanceDao.getWorkflowInstanceType(instance.parentWorkflowId)).thenReturn("parentType");
    TestDefinition parentDefinition = mock(TestDefinition.class);
    doReturn(parentDefinition).when(workflowDefinitions).getWorkflowDefinition("parentType");
    when(parentDefinition.getStates()).thenReturn(new TestDefinition("parentType", START_1).getStates());
    when(workflowInstanceDao.wakeUpWorkflowExternally(999, emptyList())).thenReturn(false);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).wakeUpWorkflowExternally(999, emptyList());
  }

  @Test
  public void finishingChildWakesParentAutomaticallyWhenParentIsInWaitState() {
    WorkflowInstance instance = executingInstanceBuilder().setParentWorkflowId(999L).setType(SIMPLE_TYPE)
        .setState(TestState.PROCESS).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstanceDao.getWorkflowInstanceType(instance.parentWorkflowId)).thenReturn(BULK_WORKFLOW_TYPE);
    BulkWorkflow parentDefinition = new BulkWorkflow();
    doReturn(parentDefinition).when(workflowDefinitions).getWorkflowDefinition(BULK_WORKFLOW_TYPE);
    when(workflowInstanceDao.wakeUpWorkflowExternally(999, singletonList(WAIT_FOR_CHILDREN_TO_FINISH.name()))).thenReturn(true);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).wakeUpWorkflowExternally(999, singletonList(WAIT_FOR_CHILDREN_TO_FINISH.name()));

  }

  @Test
  public void goToErrorStateWhenNextStateIsInvalid() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    executor = new WorkflowStateProcessor(1, shutdownRequest::get, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        maintenanceDao, workflowInstancePreProcessor, env, processingInstances, nflowLogger, stateSaveExceptionAnalyzer,
        listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(FailingTestWorkflow.INVALID_NEXT_STATE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.ERROR, 0, is("Scheduled by previous state invalidNextState")));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.INVALID_NEXT_STATE, is(
        "State 'invalidNextState' handler method returned invalid next state '" + SimpleTestWorkflow.ILLEGAL_STATE_CHANGE + "'"),
        0, stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(manual, TestState.ERROR, 0, is("Stopped in state error"), is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(TestState.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void continueProcessingWhenListenerThrowsException() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(FailingTestWorkflow.RETRYING_STATE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).beforeProcessing(any(ListenerContext.class));
    doThrow(RuntimeException.class).when(listener1).afterProcessing(any(ListenerContext.class));

    runExecutorWithTimeout();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.RETRYING_STATE, 1, is("Retrying"))),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.RETRYING_STATE, is("Retrying"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void continueProcessingWhenAfterFailureListenerThrowsException() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));

    runExecutorWithTimeout();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 1, containsString("test-fail"))),
        argThat(matchesWorkflowInstanceAction(TestState.BEGIN, containsString("test-fail"), 0, stateExecutionFailed)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void clearNextActivationWhenMovingToStateThatHasNoMethod() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE)
        .setState(FailingTestWorkflow.NEXT_STATE_NO_METHOD).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(finished, FailingTestWorkflow.NO_METHOD_END_STATE, 0,
            is("Stopped in state noMethodEndState"), is(nullValue(DateTime.class)))),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.NEXT_STATE_NO_METHOD, is("Go to end state that has no method"),
            0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void keepCurrentStateOnRetry() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(FailingTestWorkflow.RETRYING_STATE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.RETRYING_STATE, 1, is("Retrying"))),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.RETRYING_STATE, is("Retrying"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @SuppressWarnings("serial")
  @Test
  public void runWorkflowWithParameters() {
    Map<String, String> startState = new LinkedHashMap<>() {
      {
        put("string", "Str");
        put("int", "42");
        put("pojo", "{\"field\": \"val\", \"test\": true}");
        put("immutablePojo", "{\"field\": \"unmodified\"}");
      }
    };
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.PROCESS)
        .setStateVariables(startState).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(),
        argThat(matchesWorkflowInstanceAction(TestState.PROCESS, is("Finished"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));

    assertThat((String) lastArgs.get(0), is("Str"));
    assertThat((Integer) lastArgs.get(1), is(42));
    assertThat(((Pojo) lastArgs.get(2)).field, is("val modified"));
    assertThat(((Pojo) lastArgs.get(2)).test, is(true));
    assertThat(((Pojo) lastArgs.get(4)).field, is("unmodified ignored"));
    assertThat((Integer) lastArgs.get(5), is(0));
    assertThat(lastArgs.get(6), nullValue());
    Map<String, String> state = update.getValue().stateVariables;
    assertThat(state.get("pojo"), is("{\"field\":\"val modified\",\"test\":true}"));
    assertThat(state.get("nullPojo"), is("{\"field\":\"magical instance\",\"test\":false}"));
    assertThat(state.get("immutablePojo"), is("{\"field\": \"unmodified\"}"));
    assertThat(state.get("mutableString"), is("mutated"));
    assertThat(state.get("genericsMutable"), is("{\"mutated\":\"true\"}"));
    assertThat(state.get("hello"), is("[1,2,3]"));
  }

  private Matcher<WorkflowInstance> matchesWorkflowInstance(WorkflowInstanceStatus status, WorkflowState state, int retries,
      Matcher<String> stateTextMatcher) {
    return matchesWorkflowInstance(status, state, retries, stateTextMatcher, Matchers.any(DateTime.class));
  }

  private Matcher<WorkflowInstance> matchesWorkflowInstance(WorkflowInstanceStatus status, WorkflowState state, int retries,
      Matcher<String> stateTextMatcher, Matcher<? super DateTime> nextActivationMatcher) {
    return matchesWorkflowInstance(status, state, retries, stateTextMatcher, nextActivationMatcher, Matchers.any(DateTime.class));
  }

  private Matcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowInstanceStatus status, final WorkflowState state,
      final int retries, final Matcher<String> stateTextMatcher, final Matcher<? super DateTime> nextActivationMatcher,
      final Matcher<? super DateTime> startedMatcher) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {

      }

      @Override
      protected boolean matchesSafely(WorkflowInstance i) {
        assertThat(i, notNullValue());
        assertThat(i.status, is(status));
        assertThat(i.state, is(state.name()));
        assertThat(i.stateText, stateTextMatcher);
        assertThat(i.retries, is(retries));
        assertThat(i.nextActivation, nextActivationMatcher);
        assertThat(i.started, startedMatcher);
        return true;
      }
    };
  }

  private Matcher<WorkflowInstanceAction> matchesWorkflowInstanceAction(final WorkflowState state,
      final Matcher<String> stateTextMatcher, final int retryNo, final WorkflowActionType type) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {

      }

      @Override
      protected boolean matchesSafely(WorkflowInstanceAction a) {
        assertThat(a, notNullValue());
        assertThat(a.state, is(state.name()));
        assertThat(a.stateText, stateTextMatcher);
        assertThat(a.retryNo, is(retryNo));
        assertThat(a.type, is(type));
        return true;
      }
    };
  }

  private ArgumentMatcher<List<WorkflowInstance>> isEmptyWorkflowList() {
    return a -> {
      assertThat(a, empty());
      return true;
    };
  }

  @Test
  public void beforeAndAfterListenersAreExecutedForSuccessfulProcessing() {
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener2).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verify(listener2).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1, listener2);
  }

  @Test
  public void failureListenersAreExecutedAfterFailure() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAILING_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener2).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), argThat(new IsTestFailException()));
    verify(listener2).afterFailure(any(ListenerContext.class), argThat(new IsTestFailException()));
    verifyNoMoreInteractions(listener1, listener2);
  }

  static final class IsTestFailException implements ArgumentMatcher<Throwable> {
    @Override
    public boolean matches(Throwable t) {
      assertThat(t.getMessage(), is("test-fail"));
      return true;
    }
  }

  @Test
  public void instanceIsRemovedFromProcessingInstancesAfterExecution() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    assertThat(processingInstances.containsKey(instance.id), is(false));
  }

  @Test
  public void instanceWithUnsupportedTypeIsRescheduled() {
    WorkflowInstance instance = executingInstanceBuilder().setType("unsupported").setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowDefinitions.getWorkflowDefinition(instance.type)).thenReturn(null);
    DateTime oneHourInFuture = now().plusHours(1);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstance(argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 0,
        is("Unsupported workflow type"), greaterThanOrEqualTo(oneHourInFuture), is(nullValue()))));
  }

  @Test
  public void instanceWithStoredInstanceTypeIsRescheduled() {
    WorkflowInstance instance = executingInstanceBuilder().setType("unsupported").setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowDefinitions.getWorkflowDefinition(instance.type)).thenReturn(mock(StoredWorkflowDefinitionWrapper.class));
    DateTime oneHourInFuture = now().plusHours(1);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstance(argThat(matchesWorkflowInstance(inProgress, TestState.BEGIN, 0,
            is("Unsupported workflow type"), greaterThanOrEqualTo(oneHourInFuture), is(nullValue()))));
  }

  @Test
  public void illegalStateChangeGoesToErrorState() {
    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, SimpleTestWorkflow.ERROR, 0, is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE,
            is("Illegal state transition from illegalStateChange to " + TestState.BEGIN + ", proceeding to error state "
                + SimpleTestWorkflow.ERROR),
            0, stateExecutionFailed));
    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, SimpleTestWorkflow.ERROR, 0, is("Stopped in state error"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.ERROR, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void illegalStateChangeGoesToIllegalStateWhenActionIsLog() {
    env.setProperty("nflow.illegal.state.change.action", "log");
    executor = new WorkflowStateProcessor(1, shutdownRequest::get, objectMapper, workflowDefinitions, workflowInstances,
        workflowInstanceDao, maintenanceDao, workflowInstancePreProcessor, env, processingInstances, nflowLogger,
        stateSaveExceptionAnalyzer, listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, times(3)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), anyBoolean());
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.BEGIN, 0, is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE, is("illegal state change"), 0, stateExecution));
  }

  @Test
  public void illegalStateChangeGoesToIllegalStateWhenActionIsIgnore() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    executor = new WorkflowStateProcessor(1, shutdownRequest::get, objectMapper, workflowDefinitions, workflowInstances,
        workflowInstanceDao, maintenanceDao, workflowInstancePreProcessor, env, processingInstances, nflowLogger,
        stateSaveExceptionAnalyzer, listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType(SIMPLE_TYPE).setState(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE)
        .build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao, times(3)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), anyBoolean());
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, TestState.BEGIN, 0, is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE, is("illegal state change"), 0, stateExecution));
  }

  @Test
  public void stateProcessingRetryAfterFailedGetWorkflow() throws InterruptedException {
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.BEGIN).build();
    doThrow(new RuntimeException("some failure")).when(workflowInstances).getWorkflowInstance(instance.id, INCLUDES, null);

    ExecutorService executorService = newSingleThreadExecutor();
    executorService.submit(executor);
    sleep(1500);
    executorService.shutdown();
    shutdownRequest.set(true);

    verify(workflowInstances, atLeast(2)).getWorkflowInstance(instance.id, INCLUDES, null);
  }

  @Test
  public void saveStateRetryAfterFailedPersistence() throws InterruptedException {
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(new RuntimeException("some failure")).when(workflowInstanceDao).updateWorkflowInstanceAfterExecution(any(), any(),
        any(), any(), anyBoolean());

    ExecutorService executorService = newSingleThreadExecutor();
    executorService.submit(executor);
    sleep(1500);
    executorService.shutdown();
    shutdownRequest.set(true);

    verify(workflowInstanceDao, atLeast(2)).updateWorkflowInstanceAfterExecution(any(), any(), any(), any(), anyBoolean());
  }

  @Test
  public void stateProcessingSeriesStopsOnShutdown() throws InterruptedException {
    WorkflowInstance instance = executingInstanceBuilder().setType(LOOPING_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    ExecutorService executorService = newSingleThreadExecutor();
    executorService.submit(executor);
    sleep(500);

    executorService.shutdown();
    shutdownRequest.set(true);
    boolean wasTerminated = executorService.awaitTermination(5, SECONDS);
    executorService.shutdownNow();

    assertTrue(wasTerminated);
  }

  @Test
  public void deleteWorkflowInstanceHistoryNotExecutedWhenDisabled() {
    WorkflowInstance instance = executingInstanceBuilder().setType(NEVER_CLEAN_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();

    verify(maintenanceDao, never()).deleteActionAndStateHistory(anyLong(), any());
  }

  @Test
  public void deleteWorkflowInstanceHistoryExecutedWhenForced() {
    WorkflowInstance instance = executingInstanceBuilder().setType(ALWAYS_CLEAN_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();

    verify(maintenanceDao).deleteActionAndStateHistory(instance.id,
        now().minus(alwaysCleanWf.getSettings().historyDeletableAfter));
  }

  @Test
  public void deleteWorkflowInstanceHistoryExecutedBasedOnSettings() {
    WorkflowInstance instance = executingInstanceBuilder().setType(EXECUTE_TEST_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();

    verify(maintenanceDao).deleteActionAndStateHistory(instance.id, now().minus(executeWf.getSettings().historyDeletableAfter));
  }

  @Test
  public void handleDeleteWorkflowInstanceHistoryFailures() {
    WorkflowInstance instance = executingInstanceBuilder().setType(FAIL_CLEANING_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(), childWorkflows.capture(),
        workflows.capture(), anyBoolean());
  }

  @Test
  public void goToErrorStateWhenRetryIsNotAllowed() {
    WorkflowInstance instance = executingInstanceBuilder().setType(NON_RETRYABLE_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    runExecutorWithTimeout();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), anyBoolean());
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(finished, TestState.DONE, 0, is("Stopped in state done"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(TestState.BEGIN,
        containsString("Handler threw an exception and retrying is not allowed"), 0, stateExecutionFailed));
  }

  private void runExecutorWithTimeout() {
    assertTimeoutPreemptively(ofSeconds(5), executor::run);
  }

  @Test
  public void handlePotentiallyStuckCallsListeners() {
    Duration processingTime = standardHours(1);

    executor.handlePotentiallyStuck(processingTime);

    verify(listener1).handlePotentiallyStuck(null, processingTime);
    verify(listener2).handlePotentiallyStuck(null, processingTime);
  }

  @Test
  public void handlePotentiallyStuckInterruptsThreadWhenListenerReturnsTrue() throws InterruptedException {
    Duration processingTime = standardHours(1);
    when(listener1.handlePotentiallyStuck(any(ListenerContext.class), eq(processingTime))).thenReturn(true);
    WorkflowInstance instance = executingInstanceBuilder().setType(STUCK_TYPE).setState(TestState.BEGIN).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    Thread thread = new Thread(executor::run);
    thread.start();

    // let the workflow process until it is in stuck in the sleep
    sleep(500);

    executor.handlePotentiallyStuck(processingTime);

    thread.join(1000);
    assertFalse(thread.isAlive(), "Processing thread did not die after interruption");

    verify(listener1).handlePotentiallyStuck(any(ListenerContext.class), eq(processingTime));
    verify(listener2).handlePotentiallyStuck(any(ListenerContext.class), eq(processingTime));

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(), childWorkflows.capture(),
        workflows.capture(), eq(true));
    assertThat(action.getValue().type, is(stateExecutionFailed));
    assertThat(action.getValue().stateText, containsString("InterruptedException"));
  }

  public static class Pojo {
    public String field;
    public boolean test;
  }

  static List<Object> lastArgs;

  public static class ExecuteTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String EXECUTE_TEST_TYPE = "executeTest";

    protected ExecuteTestWorkflow() {
      super("test", TestState.BEGIN, TestState.ERROR,
          new WorkflowSettings.Builder().setHistoryDeletableAfter(hours(1)).setDeleteHistoryCondition(() -> true).build());
      permit(TestState.BEGIN, TestState.PROCESS, TestState.ERROR);
      permit(TestState.PROCESS, TestState.DONE, TestState.ERROR);
    }

    public NextAction begin(StateExecution execution) {
      execution.addChildWorkflows(newChildWorkflow);
      execution.addWorkflows(newWorkflow);
      return moveToStateAfter(TestState.PROCESS, getSettings().getErrorTransitionActivation(0), "Process after delay");
    }

    public NextAction process(StateExecution execution, @StateVar("string") String s, @StateVar("int") int i,
        @StateVar("pojo") Pojo pojo, @StateVar(value = "nullPojo", instantiateIfNotExists = true) Pojo pojo2,
        @StateVar(value = "immutablePojo", readOnly = true) Pojo unmodifiablePojo, @StateVar("nullInt") int zero,
        @StateVar("mutableString") Mutable<String> mutableString,
        @StateVar(value="genericsMutable", instantiateIfNotExists = true) Mutable<Map<String, String>> genericsMutable) {
      assertThat(execution.getWorkflowInstanceId(), is(1L));
      assertThat(execution.getWorkflowInstanceExternalId(), is(notNullValue()));
      Pojo pojo1 = execution.getVariable("pojo", Pojo.class);
      assertThat(pojo.field, is(pojo1.field));
      assertThat(pojo.test, is(pojo1.test));
      lastArgs = asList(s, i, pojo, pojo2, unmodifiablePojo, zero, mutableString.val, genericsMutable.val);
      pojo.field += " modified";
      pojo2.field = "magical instance";
      unmodifiablePojo.field += " ignored";
      mutableString.val = "mutated";
      genericsMutable.val.put("mutated", "true");
      execution.setVariable("hello", new int[] { 1, 2, 3 });
      return stopInState(TestState.DONE, "Finished");
    }
  }

  public static class FailingTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String FAILING_TYPE = "failingTest";

    public static final WorkflowState PROCESS_RETURN_NULL = new State("processReturnNull");
    public static final WorkflowState PROCESS_RETURN_NULL_NEXT_STATE = new State("processReturnNullNextState");
    public static final WorkflowState NEXT_STATE_NO_METHOD = new State("nextStateNoMethod");
    public static final WorkflowState NO_METHOD_END_STATE = new State("noMethodEndState", WorkflowStateType.end);
    public static final WorkflowState RETRYING_STATE = new State("retryingState");
    public static final WorkflowState INVALID = new State("invalid", WorkflowStateType.manual);
    public static final WorkflowState INVALID_NEXT_STATE = new State("invalidNextState");

    protected FailingTestWorkflow() {
      super(FAILING_TYPE, TestState.BEGIN, TestState.ERROR);
      permit(TestState.BEGIN, TestState.PROCESS, TestState.ERROR);
      permit(NEXT_STATE_NO_METHOD, NO_METHOD_END_STATE);
    }

    public NextAction begin(StateExecution execution) {
      execution.addChildWorkflows(newChildWorkflow);
      execution.addWorkflows(newWorkflow);
      throw new RuntimeException("test-fail");
    }

    public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
      throw new RuntimeException("test-fail2");
    }

    public NextAction processReturnNull(@SuppressWarnings("unused") StateExecution execution) {
      return null;
    }

    public NextAction retryingState(@SuppressWarnings("unused") StateExecution execution) {
      return retryAfter(now().plusYears(1), "Retrying");
    }

    public NextAction processReturnNullNextState(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(null, "This should fail");
    }

    public NextAction nextStateNoMethod(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(NO_METHOD_END_STATE, "Go to end state that has no method");
    }

    public NextAction invalidNextState(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(SimpleTestWorkflow.ILLEGAL_STATE_CHANGE, "illegal next state");
    }

    public void error(@SuppressWarnings("unused") StateExecution execution) {
      // tests assume this state method exists
    }
  }

  public static class NeverCleanTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String NEVER_CLEAN_TYPE = "neverCleanTest";

    protected NeverCleanTestWorkflow() {
      super(NEVER_CLEAN_TYPE, TestState.BEGIN, TestState.ERROR,
          new WorkflowSettings.Builder().setDeleteHistoryCondition(FALSE::booleanValue).build());
      permit(TestState.BEGIN, TestState.DONE, TestState.ERROR);
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(TestState.DONE, "Done.");
    }
  }

  public static class AlwaysCleanTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String ALWAYS_CLEAN_TYPE = "alwaysCleanTest";

    protected AlwaysCleanTestWorkflow() {
      super(ALWAYS_CLEAN_TYPE, TestState.BEGIN, TestState.ERROR,
          new WorkflowSettings.Builder().setDeleteHistoryCondition(FALSE::booleanValue).build());
      permit(TestState.BEGIN, TestState.DONE, TestState.ERROR);
    }

    public NextAction begin(StateExecution execution) {
      execution.setHistoryCleaningForced(true);
      return moveToState(TestState.DONE, "Done.");
    }
  }

  public static class FailCleaningTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String FAIL_CLEANING_TYPE = "failCleaningTest";

    protected FailCleaningTestWorkflow() {
      super(FAIL_CLEANING_TYPE, TestState.BEGIN, TestState.ERROR,
          new WorkflowSettings.Builder().setHistoryDeletableAfter(Period.ZERO).setDeleteHistoryCondition(() -> {
            throw new RuntimeException();
          }).build());
      permit(TestState.BEGIN, TestState.DONE, TestState.ERROR);
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(TestState.DONE, "Done.");
    }
  }

  public static class LoopingTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String LOOPING_TYPE = "loopingTest";

    protected LoopingTestWorkflow() {
      super(LOOPING_TYPE, TestState.BEGIN, TestState.ERROR);
      permit(TestState.BEGIN, TestState.BEGIN);
      permit(TestState.BEGIN, TestState.DONE);
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) throws InterruptedException {
      sleep(100);
      return moveToState(TestState.BEGIN, "loop");
    }
  }

  public static class SimpleTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String SIMPLE_TYPE = "simpleTest";
    public static final WorkflowState ERROR = new State("error", WorkflowStateType.end);
    public static final WorkflowState BEFORE_MANUAL = new State("beforeManual");
    public static final WorkflowState MANUAL = new State("manualState", WorkflowStateType.manual);
    public static final WorkflowState ILLEGAL_STATE_CHANGE = new State("illegalStateChange");

    protected SimpleTestWorkflow() {
      super(SIMPLE_TYPE, TestState.BEGIN, ERROR);
      permit(TestState.BEGIN, TestState.PROCESS);
      permit(TestState.PROCESS, TestState.DONE);
      permit(BEFORE_MANUAL, MANUAL);
    }

    public NextAction beforeManual(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(MANUAL, "Move to manual state.");
    }

    public NextAction begin(StateExecution execution) {
      assertThat(processingInstances.containsKey(execution.getWorkflowInstanceId()), is(true));
      return moveToState(TestState.PROCESS, "Move to processing.");
    }

    public NextAction process(StateExecution execution) {
      execution.setCreateAction(false);
      return stopInState(TestState.DONE, "Finished.");
    }

    public NextAction illegalStateChange(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(TestState.BEGIN, "illegal state change");
    }

    public void error(@SuppressWarnings("unused") StateExecution execution) {
      System.err.println("Executing error state");
    }
  }

  public static class NotifyTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String NOTIFY_TYPE = "notifyTest";
    public static final WorkflowState WAKE_PARENT = new State("wakeParent");

    protected NotifyTestWorkflow() {
      super(NOTIFY_TYPE, TestState.BEGIN, TestState.DONE);
      permit(TestState.BEGIN, WAKE_PARENT);
      permit(WAKE_PARENT, TestState.DONE);
    }

    public NextAction wakeParent(StateExecution execution) {
      execution.wakeUpParentWorkflow();
      return moveToState(TestState.DONE, "Wake up parent");
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(WAKE_PARENT, "Move to notifyParent.");
    }
  }

  public static class StateVariableTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String STATE_VARIABLE_TYPE = "stateVariableTest";

    protected StateVariableTestWorkflow() {
      super(STATE_VARIABLE_TYPE, TestState.BEGIN, TestState.DONE);
      permit(TestState.BEGIN, TestState.DONE);
    }

    public NextAction begin(StateExecution execution) {
      execution.setVariable("foo", "bar");
      return moveToState(TestState.DONE, "Done.");
    }
  }

  public static class NonRetryableTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String NON_RETRYABLE_TYPE = "nonRetryableTest";

    protected NonRetryableTestWorkflow() {
      super(NON_RETRYABLE_TYPE, TestState.BEGIN, TestState.DONE, new WorkflowSettings.Builder()
          .setExceptionAnalyzer((s, t) -> new StateProcessExceptionHandling.Builder().setRetryable(false).build()).build());
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
      throw new RuntimeException();
    }
  }

  public static class StuckTestWorkflow extends io.nflow.engine.workflow.definition.WorkflowDefinition {

    public static final String STUCK_TYPE = "stuckTest";

    protected StuckTestWorkflow() {
      super(STUCK_TYPE, TestState.BEGIN, TestState.DONE);
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) throws InterruptedException {
      SECONDS.sleep(10);
      return stopInState(TestState.DONE, "Done");
    }
  }
}
