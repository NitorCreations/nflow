package io.nflow.engine.internal.executor;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.manual;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.StateExecutionImpl;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.listener.ListenerChain;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.TestWorkflow;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowDefinitionTest.TestDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

public class WorkflowStateProcessorTest extends BaseNflowTest {

  @Rule
  public Timeout timeoutPerMethod = Timeout.seconds(5);

  @Mock
  WorkflowDefinitionService workflowDefinitions;

  @Mock
  WorkflowInstanceService workflowInstances;

  @Mock
  WorkflowInstanceDao workflowInstanceDao;

  MockEnvironment env = new MockEnvironment();

  @Mock
  WorkflowExecutorListener listener1;

  @Mock
  WorkflowExecutorListener listener2;

  @Mock
  WorkflowInstancePreProcessor workflowInstancePreProcessor;

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

  WorkflowDefinition<ExecuteTestWorkflow.State> executeWf = new ExecuteTestWorkflow();

  WorkflowDefinition<ForceCleaningTestWorkflow.State> forceWf = new ForceCleaningTestWorkflow();

  WorkflowDefinition<FailCleaningTestWorkflow.State> failCleaningWf = new FailCleaningTestWorkflow();

  WorkflowDefinition<SimpleTestWorkflow.State> simpleWf = new SimpleTestWorkflow();

  WorkflowDefinition<FailingTestWorkflow.State> failingWf = new FailingTestWorkflow();

  WorkflowDefinition<NotifyTestWorkflow.State> wakeWf = new NotifyTestWorkflow();

  static WorkflowInstance newChildWorkflow = mock(WorkflowInstance.class);

  static WorkflowInstance newWorkflow = mock(WorkflowInstance.class);

  static Map<Integer, WorkflowStateProcessor> processingInstances;

  private final TestWorkflow testWorkflowDef = new TestWorkflow();

  private final DateTime tomorrow = now().plusDays(1);

  private final Set<WorkflowInstanceInclude> INCLUDES = EnumSet.of(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS,
      WorkflowInstanceInclude.CURRENT_STATE_VARIABLES, WorkflowInstanceInclude.STARTED);

  @Before
  public void setup() {
    processingInstances = new ConcurrentHashMap<>();
    env.setProperty("nflow.illegal.state.change.action", "fail");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    env.setProperty("nflow.executor.stateSaveRetryDelay.seconds", "1");

    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listener1, listener2);
    setCurrentMillisFixed(currentTimeMillis());
    doReturn(executeWf).when(workflowDefinitions).getWorkflowDefinition("execute-test");
    doReturn(forceWf).when(workflowDefinitions).getWorkflowDefinition("force-test");
    doReturn(failCleaningWf).when(workflowDefinitions).getWorkflowDefinition("fail-cleaning-test");
    doReturn(simpleWf).when(workflowDefinitions).getWorkflowDefinition("simple-test");
    doReturn(failingWf).when(workflowDefinitions).getWorkflowDefinition("failing-test");
    doReturn(wakeWf).when(workflowDefinitions).getWorkflowDefinition("wake-test");
    filterChain(listener1);
    filterChain(listener2);
    when(executionMock.getRetries()).thenReturn(testWorkflowDef.getSettings().maxRetries);
  }

  @After
  public void cleanUp() {
    setCurrentMillisSystem();
  }

  @Test
  public void runWorkflowThroughOneSuccessfulState() {
    WorkflowInstance instance = executingInstanceBuilder().setType("execute-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstancePreProcessor.process(newChildWorkflow)).thenReturn(newChildWorkflow);
    when(workflowInstancePreProcessor.process(newWorkflow)).thenReturn(newWorkflow);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(
            matchesWorkflowInstance(inProgress, ExecuteTestWorkflow.State.process, 0, is("Scheduled by previous state start"))),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(ExecuteTestWorkflow.State.start, is("Process after delay"), 0, stateExecution)),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(childWorkflows.getValue().get(0), is(newChildWorkflow));
    assertThat(workflows.getValue().get(0), is(newWorkflow));
  }

  @Test
  public void runWorkflowThroughOneFailedState() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.start, 1, containsString("test-fail"))),
        MockitoHamcrest.argThat(
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, containsString("test-fail"), 0, stateExecutionFailed)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void runWorkflowThroughToFailureState() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("start")
        .setRetries(failingWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(update.getAllValues().get(0),
        matchesWorkflowInstance(executing, FailingTestWorkflow.State.failure, 0,
            is("Max retry count exceeded, going to failure state")));
    assertThat(action.getAllValues().get(0),
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, is("Max retry count exceeded, going to failure state"),
                    failingWf.getSettings().maxRetries,
                    stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
            matchesWorkflowInstance(manual, FailingTestWorkflow.State.failure, 0, is("Stopped in state failure"),
                    nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.failure, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void workflowStatusIsSetToExecutingWhenNextStateIsProcessedImmediately() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), anyBoolean());
    assertThat(update.getAllValues().get(0),
            matchesWorkflowInstance(executing, SimpleTestWorkflow.State.processing, 0, is("Scheduled by previous state start")));
    assertThat(action.getAllValues().get(0),
            matchesWorkflowInstanceAction(SimpleTestWorkflow.State.start, is("Move to processing."), 0, stateExecution));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, SimpleTestWorkflow.State.end, 0, is("Stopped in state end"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.State.processing, is("Finished."), 0, stateExecution));
  }

  @Test
  public void actionIsNotCreatedWhenCreateActionIsSetToFalse() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("processing").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(), childWorkflows.capture(),
        workflows.capture(), eq(false));
  }

  @Test
  public void workflowStatusIsSetToFinishedForFinalStates() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), anyBoolean());
    assertThat(update.getAllValues().get(1),
            matchesWorkflowInstance(finished, SimpleTestWorkflow.State.end, 0, is("Stopped in state end"), nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
            matchesWorkflowInstanceAction(SimpleTestWorkflow.State.processing, is("Finished."), 0, stateExecution));
  }

  @Test
  public void instanceWithUnsupportedStateIsRescheduled() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("invalid").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    DateTime oneHourInFuture = now().plusHours(1);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstance(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.invalid, 0,
            is("Unsupported workflow state"), greaterThanOrEqualTo(oneHourInFuture))));
  }

  @Test
  public void workflowStatusIsSetToManualForManualStates() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("beforeManual")
        .setRetries(simpleWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(manual, SimpleTestWorkflow.State.manualState, 0, is("Stopped in state manualState"),
            nullValue(DateTime.class))),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(SimpleTestWorkflow.State.beforeManual, is("Move to manual state."),
            simpleWf.getSettings().maxRetries, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void maxRetryIsObeyedForManualRetry() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("retryingState")
        .setRetries(failingWf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(
            update.getAllValues().get(0),
            matchesWorkflowInstance(executing, FailingTestWorkflow.State.error, 0,
                    is("Max retry count exceeded, no failure state defined, going to error state")));
    assertThat(
            action.getAllValues().get(0),
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.retryingState,
                    is("Max retry count exceeded, no failure state defined, going to error state"), failingWf.getSettings().maxRetries,
                    stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
            matchesWorkflowInstance(finished, FailingTestWorkflow.State.error, 0, is("Stopped in state error"),
                    is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void skippingWorkflowWithListenerCausesProcessorToStopProcessingWorkflow() {
    DateTime now = now();
    final DateTime skipped = now.plusHours(1);
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setNextActivation(now).setState("start").setStateText("myStateText").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    WorkflowExecutorListener listener = mock(WorkflowExecutorListener.class);
    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listener);

    doAnswer(new Answer<NextAction>() {
      @Override
      public NextAction answer(InvocationOnMock invocation) {
        return retryAfter(skipped, "");
      }
    }).when(listener).process(any(ListenerContext.class), any(ListenerChain.class));

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstance(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, SimpleTestWorkflow.State.start, 0, is("Scheduled by previous state start"),
            is(skipped))));
  }

  @Test
  public void goToErrorStateWhenStateMethodReturnsNull() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("processReturnNull").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    filterChain(listener1);
    executor.run();
    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1, times(2)).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), isNull());
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, FailingTestWorkflow.State.error, 0,
            is("Scheduled by previous state processReturnNull")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.processReturnNull, is("State handler method returned null"), 0,
            stateExecutionFailed));

    assertThat(
        update.getAllValues().get(1),
        matchesWorkflowInstance(finished, FailingTestWorkflow.State.error, 0, is("Stopped in state error"),
            is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, is("Stopped in final state"), 0, stateExecution));
  }

  private void filterChain(WorkflowExecutorListener listener) {
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        ListenerContext context = (ListenerContext) invocation.getArguments()[0];
        ListenerChain chain = (ListenerChain) invocation.getArguments()[1];
        chain.next(context);
        return null;
      }
    }).when(listener).process(any(ListenerContext.class), any(ListenerChain.class));
  }

  @Test
  public void goToErrorStateWhenNextStateIsNull() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("processReturnNullNextState").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1, times(2)).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, FailingTestWorkflow.State.error, 0,
            is("Scheduled by previous state processReturnNullNextState")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.processReturnNullNextState, is("Next state can not be null"), 0,
            stateExecutionFailed));

    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, FailingTestWorkflow.State.error, 0, is("Stopped in state error"),
            is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, is("Stopped in final state"), 0, stateExecution));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void doNothingWhenNotifyingParentWithoutParentWorkflowId() {
    WorkflowInstance instance = executingInstanceBuilder().setType("wake-test").setState("wakeParent").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao, never()).wakeUpWorkflowExternally(any(Integer.class), any(List.class));
  }

  @Test
  public void whenWakingUpParentWorkflowSucceeds() {
    WorkflowInstance instance = executingInstanceBuilder().setParentWorkflowId(999).setType("wake-test").setState("wakeParent").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstanceDao.wakeUpWorkflowExternally(999, new ArrayList<String>())).thenReturn(true);
    executor.run();
    verify(workflowInstanceDao).wakeUpWorkflowExternally(999, new ArrayList<String>());
  }

  @Test
  public void whenWakingUpParentWorkflowFails() {
    WorkflowInstance instance = executingInstanceBuilder().setParentWorkflowId(999).setType("wake-test").setState("wakeParent").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowInstanceDao.wakeUpWorkflowExternally(999, new ArrayList<String>())).thenReturn(false);
    executor.run();
    verify(workflowInstanceDao).wakeUpWorkflowExternally(999, new ArrayList<String>());
  }

  @Test
  public void goToErrorStateWhenNextStateIsInvalid() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("invalidNextState").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        childWorkflows.capture(), workflows.capture(), eq(true));
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, FailingTestWorkflow.State.error, 0, is("Scheduled by previous state invalidNextState")));
    assertThat(
        action.getAllValues().get(0),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.invalidNextState,
            is("State 'invalidNextState' handler method returned invalid next state 'illegalStateChange'"), 0,
            stateExecutionFailed));

    assertThat(
        update.getAllValues().get(1),
        matchesWorkflowInstance(finished, FailingTestWorkflow.State.error, 0, is("Stopped in state error"),
            is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void continueProcessingWhenListenerThrowsException() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("retryingState").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).beforeProcessing(any(ListenerContext.class));
    doThrow(RuntimeException.class).when(listener1).afterProcessing(any(ListenerContext.class));

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.retryingState, 1, is("Retrying"))),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.retryingState, is("Retrying"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void continueProcessingWhenAfterFailureListenerThrowsException() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.start, 1, containsString("test-fail"))),
        MockitoHamcrest.argThat(
            matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, containsString("test-fail"), 0, stateExecutionFailed)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void clearNextActivationWhenMovingToStateThatHasNoMethod() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("nextStateNoMethod").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(finished, FailingTestWorkflow.State.noMethodEndState, 0,
            is("Stopped in state noMethodEndState"), is(nullValue(DateTime.class)))),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.nextStateNoMethod,
            is("Go to end state that has no method"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @Test
  public void keepCurrentStateOnRetry() {
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("retryingState").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.retryingState, 1, is("Retrying"))),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.retryingState, is("Retrying"), 0, stateExecution)),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
  }

  @SuppressWarnings("serial")
  @Test
  public void runWorkflowWithParameters() {
    Map<String, String> startState = new LinkedHashMap<String, String>() {{
      put("string", "Str");
      put("int", "42");
      put("pojo", "{\"field\": \"val\", \"test\": true}");
      put("immutablePojo", "{\"field\": \"unmodified\"}");
    }};
    WorkflowInstance instance = executingInstanceBuilder().setType("execute-test").setState("process")
        .setStateVariables(startState).build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(),
        MockitoHamcrest.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.process, is("Finished"), 0, stateExecution)),
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
    assertThat(state.get("hello"), is("[1,2,3]"));
  }

  private Matcher<WorkflowInstance> matchesWorkflowInstance(WorkflowInstanceStatus status, WorkflowState state,
      int retries, Matcher<String> stateTextMatcher) {
    return matchesWorkflowInstance(status, state, retries, stateTextMatcher, Matchers.any(DateTime.class));
  }

  private Matcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowInstanceStatus status,
      final WorkflowState state, final int retries, final Matcher<String> stateTextMatcher,
      final Matcher<? super DateTime> nextActivationMatcher) {
    return new TypeSafeMatcher<WorkflowInstance>() {
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
        return true;
      }
    };
  }

  private Matcher<WorkflowInstanceAction> matchesWorkflowInstanceAction(final WorkflowState state,
      final Matcher<String> stateTextMatcher, final int retryNo, final WorkflowActionType type) {
    return new TypeSafeMatcher<WorkflowInstanceAction>() {
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
        return new ArgumentMatcher<List<WorkflowInstance>>() {
            @Override
            public boolean matches(List<WorkflowInstance> a) {
                assertThat(a, empty());
                return true;
            }
        };
    }
  @Test
  public void beforeAndAfterListenersAreExecutedForSuccessfulProcessing() {
    WorkflowInstance instance = executingInstanceBuilder().setType("execute-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

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
    WorkflowInstance instance = executingInstanceBuilder().setType("failing-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener2).process(any(ListenerContext.class), any(ListenerChain.class));
    verify(listener1).afterFailure(any(ListenerContext.class),  argThat(new IsTestFailException()));
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
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    assertThat(processingInstances.containsKey(instance.id), is(false));
  }


  @Test
  public void instanceWithUnsupportedTypeIsRescheduled() {
    WorkflowInstance instance = executingInstanceBuilder().setType("test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    when(workflowDefinitions.getWorkflowDefinition("test")).thenReturn(null);
    DateTime oneHourInFuture = now().plusHours(1);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstance(
        MockitoHamcrest.argThat(matchesWorkflowInstance(inProgress, FailingTestWorkflow.State.start, 0,
            is("Unsupported workflow type"), greaterThanOrEqualTo(oneHourInFuture))));
  }

  @Test
  public void illegalStateChangeGoesToErrorState() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("illegalStateChange").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), eq(true));
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, SimpleTestWorkflow.State.error, 0,
            is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.State.illegalStateChange,
            is("Illegal state transition from illegalStateChange to start, proceeding to error state error"), 0,
            stateExecutionFailed));
    assertThat(update.getAllValues().get(1),
        matchesWorkflowInstance(finished, SimpleTestWorkflow.State.error, 0, is("Stopped in state error"),
            nullValue(DateTime.class)));
    assertThat(action.getAllValues().get(1),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.State.error, is("Stopped in final state"), 0, stateExecution));
  }

  @Test
  public void illegalStateChangeGoesToIllegalStateWhenActionIsLog() {
    env.setProperty("nflow.illegal.state.change.action", "log");
    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("illegalStateChange").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(3)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), anyBoolean());
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, SimpleTestWorkflow.State.start, 0,
            is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.State.illegalStateChange, is("illegal state change"), 0, stateExecution));
  }

  @Test
  public void illegalStateChangeGoesToIllegalStateWhenActionIsIgnore() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listener1, listener2);

    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("illegalStateChange").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(3)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(),
        argThat(isEmptyWorkflowList()), argThat(isEmptyWorkflowList()), anyBoolean());
    assertThat(
        update.getAllValues().get(0),
        matchesWorkflowInstance(executing, SimpleTestWorkflow.State.start, 0,
            is("Scheduled by previous state illegalStateChange")));
    assertThat(action.getAllValues().get(0),
        matchesWorkflowInstanceAction(SimpleTestWorkflow.State.illegalStateChange, is("illegal state change"), 0, stateExecution));
  }

  @Test
  public void handleRetryMaxRetriesExceededHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestDefinition.TestState.start1);
    verify(execution).setNextState(TestDefinition.TestState.failed);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(any(DateTime.class));
  }

  @Test
  public void handleRetryMaxRetriesExceededNotHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestDefinition.TestState.start2);
    verify(execution).setNextState(TestDefinition.TestState.error);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(any(DateTime.class));
  }

  private StateExecutionImpl handleRetryMaxRetriesExceeded(TestDefinition.TestState currentState) {
    TestDefinition def = new TestDefinition("x", currentState);
    StateExecutionImpl execution = mock(StateExecutionImpl.class);
    when(execution.getRetries()).thenReturn(def.getSettings().maxRetries);
    when(execution.getCurrentStateName()).thenReturn(currentState.name());
    executor.handleRetry(execution, def);
    return execution;
  }

  @Test
  public void exceedingMaxRetriesInFailureStateGoesToErrorState() {
    when(executionMock.getCurrentStateName()).thenReturn(TestWorkflow.State.failed.name());

    executor.handleRetryAfter(executionMock, tomorrow, testWorkflowDef);

    verify(executionMock).setNextState(TestWorkflow.State.error);
    verify(executionMock).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInNonFailureStateGoesToFailureState() {
    when(executionMock.getCurrentStateName()).thenReturn(TestWorkflow.State.begin.name());

    executor.handleRetryAfter(executionMock, tomorrow, testWorkflowDef);

    verify(executionMock).setNextState(TestWorkflow.State.failed);
    verify(executionMock).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInNonFailureStateGoesToErrorStateWhenNoFailureStateIsDefined() {
    when(executionMock.getCurrentStateName()).thenReturn(TestWorkflow.State.startWithoutFailure.name());

    executor.handleRetryAfter(executionMock, tomorrow, testWorkflowDef);

    verify(executionMock).setNextState(TestWorkflow.State.error);
    verify(executionMock).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInErrorStateStopsProcessing() {
    when(executionMock.getCurrentStateName()).thenReturn(TestWorkflow.State.error.name());

    executor.handleRetryAfter(executionMock, tomorrow, testWorkflowDef);

    verify(executionMock).setNextState(TestWorkflow.State.error);
    verify(executionMock).setNextActivation(null);
  }

  @Test
  public void handleRetryAfterSetsActivationWhenMaxRetriesIsNotExceeded() {
    when(executionMock.getRetries()).thenReturn(0);

    executor.handleRetryAfter(executionMock, tomorrow, testWorkflowDef);

    verify(executionMock, never()).setNextState(any(TestWorkflow.State.class));
    verify(executionMock).setNextActivation(tomorrow);
  }

  @Test
  public void saveStateRetryAfterFailedPersistence() throws InterruptedException {
    WorkflowInstance instance = executingInstanceBuilder().setType("execute-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    doThrow(new RuntimeException("some failure")).when(workflowInstanceDao).updateWorkflowInstanceAfterExecution(any(), any(), any(), any(), anyBoolean());

    ExecutorService executorService = newSingleThreadExecutor();
    newSingleThreadExecutor().submit(executor);
    Thread.sleep(1500);
    executor.setStateSaveRetryEnabled(false);
    executorService.shutdownNow();

    verify(workflowInstanceDao, atLeast(2)).updateWorkflowInstanceAfterExecution(any(), any(), any(), any(), anyBoolean());
  }

  @Test
  public void deleteWorkflowInstanceHistoryNotExecutedWithDefaultSettings() {
    WorkflowInstance instance = executingInstanceBuilder().setType("simple-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();

    verify(workflowInstanceDao, never()).deleteWorkflowInstanceHistory(any(), any());
  }

  @Test
  public void deleteWorkflowInstanceHistoryExecutedWhenForced() {
    WorkflowInstance instance = executingInstanceBuilder().setType("force-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();

    verify(workflowInstanceDao).deleteWorkflowInstanceHistory(instance.id, forceWf.getSettings().historyDeletableAfterHours);
  }

  @Test
  public void deleteWorkflowInstanceHistoryExecutedBasedOnSettings() {
    WorkflowInstance instance = executingInstanceBuilder().setType("execute-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();

    verify(workflowInstanceDao).deleteWorkflowInstanceHistory(instance.id, executeWf.getSettings().historyDeletableAfterHours);
  }

  @Test
  public void handleDeleteWorkflowInstanceHistoryFailures() {
    WorkflowInstance instance = executingInstanceBuilder().setType("fail-cleaning-test").setState("start").build();
    when(workflowInstances.getWorkflowInstance(instance.id, INCLUDES, null)).thenReturn(instance);
    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture(), childWorkflows.capture(),
        workflows.capture(), anyBoolean());
  }

  public static class Pojo {
    public String field;
    public boolean test;
  }

  static List<Object> lastArgs;

  public static class ExecuteTestWorkflow extends
      WorkflowDefinition<ExecuteTestWorkflow.State> {

    protected ExecuteTestWorkflow() {
      super("test", State.start, State.error,
          new WorkflowSettings.Builder().setHistoryDeletableAfterHours(1).setDeleteHistoryCondition(() -> true).build());
      permit(State.start, State.process, State.error);
      permit(State.process, State.done, State.error);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), process(WorkflowStateType.normal), done(WorkflowStateType.end),
      error(WorkflowStateType.manual);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
          return name();
      }
    }

    public NextAction start(StateExecution execution) {
      execution.addChildWorkflows(newChildWorkflow);
      execution.addWorkflows(newWorkflow);
      return moveToStateAfter(State.process, getSettings().getErrorTransitionActivation(0), "Process after delay");
    }

    public NextAction process(StateExecution execution, @StateVar("string") String s, @StateVar("int") int i,
        @StateVar("pojo") Pojo pojo, @StateVar(value = "nullPojo", instantiateIfNotExists = true) Pojo pojo2,
        @StateVar(value = "immutablePojo", readOnly = true) Pojo unmodifiablePojo, @StateVar("nullInt") int zero,
        @StateVar("mutableString") Mutable<String> mutableString) {
      assertThat(execution.getWorkflowInstanceId(), is(1));
      assertThat(execution.getWorkflowInstanceExternalId(), is(notNullValue()));
      Pojo pojo1 = execution.getVariable("pojo", Pojo.class);
      assertThat(pojo.field, is(pojo1.field));
      assertThat(pojo.test, is(pojo1.test));
      lastArgs = asList(s, i, pojo, pojo2, unmodifiablePojo, zero, mutableString.val);
      pojo.field += " modified";
      pojo2.field = "magical instance";
      unmodifiablePojo.field += " ignored";
      mutableString.val = "mutated";
      execution.setVariable("hello", new int[]{1,2,3});
      return stopInState(State.done, "Finished");
    }
  }

  public static class FailingTestWorkflow extends
      WorkflowDefinition<FailingTestWorkflow.State> {

    protected FailingTestWorkflow() {
      super("failing", State.start, State.error);
      permit(State.start, State.process, State.failure);
      permit(State.nextStateNoMethod, State.noMethodEndState);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), process(WorkflowStateType.normal), error(WorkflowStateType.end),
      processReturnNull(WorkflowStateType.normal), processReturnNullNextState(WorkflowStateType.normal),
      nextStateNoMethod(WorkflowStateType.normal), noMethodEndState(WorkflowStateType.end),
      retryingState(WorkflowStateType.normal), failure(WorkflowStateType.manual),
      invalid(WorkflowStateType.manual), invalidNextState(WorkflowStateType.normal);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
          return name();
      }
    }

    public NextAction start(StateExecution execution) {
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

    public void failure(@SuppressWarnings("unused") StateExecution execution) {
    }

    public NextAction nextStateNoMethod(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.noMethodEndState, "Go to end state that has no method");
    }

    public NextAction invalidNextState(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(SimpleTestWorkflow.State.illegalStateChange, "illegal next state");
    }

    public void error(@SuppressWarnings("unused") StateExecution execution) {
    }
  }

  public static class ForceCleaningTestWorkflow extends WorkflowDefinition<ForceCleaningTestWorkflow.State> {

    protected ForceCleaningTestWorkflow() {
      super("test", State.start, State.error, new WorkflowSettings.Builder().setHistoryDeletableAfterHours(2).setDeleteHistoryCondition(() -> false).build());
      permit(State.start, State.done, State.error);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), done(WorkflowStateType.end), error(WorkflowStateType.manual);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
        return name();
      }
    }

    public NextAction start(StateExecution execution) {
      execution.setHistoryCleaningForced(true);
      return moveToState(State.done, "Done.");
    }

  }

  public static class FailCleaningTestWorkflow extends WorkflowDefinition<FailCleaningTestWorkflow.State> {

    protected FailCleaningTestWorkflow() {
      super("test", State.start, State.error,
          new WorkflowSettings.Builder().setHistoryDeletableAfterHours(0).setDeleteHistoryCondition(() -> {
            throw new RuntimeException();
          }).build());
      permit(State.start, State.done, State.error);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), done(WorkflowStateType.end), error(WorkflowStateType.manual);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
        return name();
      }
    }

    public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.done, "Done.");
    }

  }

  public static class SimpleTestWorkflow extends WorkflowDefinition<SimpleTestWorkflow.State> {

    protected SimpleTestWorkflow() {
      super("simple", State.start, State.error);
      permit(State.start, State.processing);
      permit(State.processing, State.end);
      permit(State.beforeManual, State.manualState);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), beforeManual(WorkflowStateType.normal), end(WorkflowStateType.end), manualState(
          WorkflowStateType.manual), error(WorkflowStateType.end), processing(WorkflowStateType.normal), illegalStateChange(
          WorkflowStateType.normal);

      private final WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
          return name();
      }
    }

    public NextAction beforeManual(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.manualState, "Move to manual state.");
    }

    public NextAction start(StateExecution execution) {
      assertThat(processingInstances.containsKey(execution.getWorkflowInstanceId()), is(true));
      return moveToState(State.processing, "Move to processing.");
    }

    public NextAction processing(StateExecution execution) {
      execution.setCreateAction(false);
      return stopInState(State.end, "Finished.");
    }

    public NextAction illegalStateChange(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.start, "illegal state change");
    }

    public void error(@SuppressWarnings("unused") StateExecution execution) {
      System.err.println("Executing error state");
    }
  }

  public static class NotifyTestWorkflow extends WorkflowDefinition<NotifyTestWorkflow.State> {

    protected NotifyTestWorkflow() {
      super("notify", State.start, State.end);
      permit(State.start, State.wakeParent);
      permit(State.wakeParent, State.end);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), wakeParent(WorkflowStateType.normal), end(WorkflowStateType.end);

      private final WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
        return name();
      }
    }

    public NextAction wakeParent(StateExecution execution) {
      execution.wakeUpParentWorkflow();
      return moveToState(State.end, "Wake up parent");
    }

    public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.wakeParent, "Move to notifyParent.");
    }

  }

}
