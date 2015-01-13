package com.nitorcreations.nflow.engine.internal.executor;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.retryAfter;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.definition.Mutable;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class WorkflowStateProcessorTest extends BaseNflowTest {

  @Mock
  WorkflowDefinitionService workflowDefinitions;

  @Mock
  WorkflowInstanceService workflowInstances;

  @Mock
  WorkflowInstanceDao workflowInstanceDao;

  @Mock
  WorkflowExecutorListener listener1;

  @Mock
  WorkflowExecutorListener listener2;

  @Captor
  ArgumentCaptor<WorkflowInstance> update;

  @Captor
  ArgumentCaptor<WorkflowInstanceAction> action;

  ObjectStringMapper objectMapper = new ObjectStringMapper(new ObjectMapper());

  WorkflowStateProcessor executor;


  @Before
  public void setup() {
    executor = new WorkflowStateProcessor(1, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        listener1, listener2);
  }

  @Test
  public void runWorkflowThroughOneSuccessfulState() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").setStateText("stateText").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(ExecuteTestWorkflow.State.process, 0, false, (String) null)),
        argThat(matchesWorkflowInstanceAction(ExecuteTestWorkflow.State.start, 0)));
  }

  @Test
  public void runWorkflowThroughOneFailedState() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 1, false)),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, 0)));
  }

  @Test
  public void runWorkflowThroughToFailureState() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").setRetries(wf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    doNothing().when(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(), action.capture());
    executor.run();
    assertThat(update.getAllValues().get(0), matchesWorkflowInstance(FailingTestWorkflow.State.failure, 0, true));
    assertThat(update.getAllValues().get(1), matchesWorkflowInstance(FailingTestWorkflow.State.failure, 0, false, is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, wf.getSettings().maxRetries));
    assertThat(action.getAllValues().get(1), matchesWorkflowInstanceAction(FailingTestWorkflow.State.failure, 0));
  }

  @Test
  public void maxRetryIsObeyedForManualRetry() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("retryingState").setRetries(wf.getSettings().maxRetries).build();
    when(workflowInstances.getWorkflowInstance(instance.id)).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture());
    assertThat(update.getAllValues().get(0), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, true));
    assertThat(update.getAllValues().get(1), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, false, is(nullValue(DateTime.class))));
    assertThat(action.getAllValues().get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.State.retryingState, wf.getSettings().maxRetries));
    assertThat(action.getAllValues().get(1), matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, 0));
  }

  @Test
  public void goToErrorStateWhenStateMethodReturnsNull() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("processReturnNull").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture());
    List<WorkflowInstance> instances = update.getAllValues();
    List<WorkflowInstanceAction> actions = action.getAllValues();

    assertThat(instances.get(0), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, true, is(notNullValue(DateTime.class))));
    assertThat(actions.get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.State.processReturnNull, 0));

    assertThat(instances.get(1), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, false, is(nullValue(DateTime.class))));
    assertThat(actions.get(1), matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, 0));
  }

  @Test
  public void goToErrorStateWhenNextStateIsNull() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("processReturnNullNextState").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(listener1, times(2)).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao, times(2)).updateWorkflowInstanceAfterExecution(update.capture(), action.capture());
    List<WorkflowInstance> instances = update.getAllValues();
    List<WorkflowInstanceAction> actions = action.getAllValues();

    assertThat(instances.get(0), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, true, is(notNullValue(DateTime.class))));
    assertThat(actions.get(0), matchesWorkflowInstanceAction(FailingTestWorkflow.State.processReturnNullNextState, 0));

    assertThat(instances.get(1), matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, false, is(nullValue(DateTime.class))));
    assertThat(actions.get(1), matchesWorkflowInstanceAction(FailingTestWorkflow.State.error, 0));
  }

  @Test
  public void continueProcessingWhenListenerThrowsException() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("retryingState").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).beforeProcessing(any(ListenerContext.class));
    doThrow(RuntimeException.class).when(listener1).afterProcessing(any(ListenerContext.class));

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(
            FailingTestWorkflow.State.retryingState, 1, false, "Retrying")), any(WorkflowInstanceAction.class));
  }

  @Test
  public void continueProcessingWhenAfterFailureListenerThrowsException() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    doThrow(RuntimeException.class).when(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class), any(Throwable.class));
    verifyNoMoreInteractions(listener1);
    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(
            FailingTestWorkflow.State.start, 1, false)), any(WorkflowInstanceAction.class));
  }

  @Test
  public void clearNextActivationWhenMovingToStateThatHasNoMethod() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("nextStateNoMethod").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(
            FailingTestWorkflow.State.noMethodEndState, 0, false,
            is(nullValue(DateTime.class)))), any(WorkflowInstanceAction.class));
  }

  @Test
  public void keepCurrentStateOnRetry() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("retryingState").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(workflowInstanceDao).updateWorkflowInstanceAfterExecution(
        argThat(matchesWorkflowInstance(
            FailingTestWorkflow.State.retryingState, 1, false,
            is(notNullValue(DateTime.class)))), any(WorkflowInstanceAction.class));
  }

  @SuppressWarnings("serial")
  @Test
  public void runWorkflowWithParameters() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    Map<String, String> startState = new LinkedHashMap<String, String>() {{
      put("string", "Str");
      put("int", "42");
      put("pojo", "{\"field\": \"val\", \"test\": true}");
      put("immutablePojo", "{\"field\": \"unmodified\"}");
    }};
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("process").setStateVariables(startState).build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    doNothing().when(workflowInstanceDao).updateWorkflowInstanceAfterExecution(update.capture(),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.process, 0)));

    executor.run();

    assertThat((String) lastArgs.get(0), is("Str"));
    assertThat((Integer) lastArgs.get(1), is(42));
    assertThat(((Pojo) lastArgs.get(2)).field, is("val modified"));
    assertThat(((Pojo) lastArgs.get(2)).test, is(true));
    assertThat(((Pojo) lastArgs.get(4)).field, is("unmodified ignored"));
    assertThat((Integer) lastArgs.get(5), is(0));
    assertThat(lastArgs.get(6), Matchers.nullValue());
    Map<String, String> state = update.getValue().stateVariables;
    assertThat(state.get("pojo"), is("{\"field\":\"val modified\",\"test\":true}"));
    assertThat(state.get("nullPojo"), is("{\"field\":\"magical instance\",\"test\":false}"));
    assertThat(state.get("immutablePojo"), is("{\"field\": \"unmodified\"}"));
    assertThat(state.get("mutableString"), is("mutated"));
    assertThat(state.get("hello"), is("[1,2,3]"));
  }

  private ArgumentMatcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowState state,
      final int retries, final boolean isProcessing) {
    return matchesWorkflowInstance(state, retries, isProcessing, CoreMatchers.any(DateTime.class));
  }

  private ArgumentMatcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowState state,
      final int retries, final boolean isProcessing, final String stateText) {
    return new ArgumentMatcher<WorkflowInstance>() {
      @Override
      public boolean matches(Object argument) {
        WorkflowInstance i = (WorkflowInstance) argument;
        assertThat(i, notNullValue());
        assertThat(i.state, equalTo(state.name()));
        assertThat(i.stateText, equalTo(stateText));
        assertThat(i.retries, is(retries));
        assertThat(i.processing, is(isProcessing));
        return true;
      }
    };
  }

  private ArgumentMatcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowState state,
      final int retries, final boolean isProcessing, final Matcher<DateTime> nextActivationMatcher) {
    return new ArgumentMatcher<WorkflowInstance>() {
      @Override
      public boolean matches(Object argument) {
        WorkflowInstance i = (WorkflowInstance) argument;
        assertThat(i, notNullValue());
        assertThat(i.state, equalTo(state.name()));
        assertThat(i.retries, is(retries));
        assertThat(i.processing, is(isProcessing));
        assertThat(i.nextActivation, nextActivationMatcher);
        return true;
      }
    };
  }

  private ArgumentMatcher<WorkflowInstanceAction> matchesWorkflowInstanceAction(final WorkflowState state, final int retryNo) {
    return new ArgumentMatcher<WorkflowInstanceAction>() {
      @Override
      public boolean matches(Object argument) {
        WorkflowInstanceAction i = (WorkflowInstanceAction) argument;
        assertThat(i.state, equalTo(state.name()));
        assertThat(i.retryNo, is(retryNo));
        return true;
      }
    };
  }

  @Test
  public void beforeAndAfterListenersAreExecutedForSuccessfulProcessing() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verify(listener2).afterProcessing(any(ListenerContext.class));
    verifyNoMoreInteractions(listener1, listener2);
  }

  @Test
  public void failureListenersAreExecutedAfterFailure() {
    FailingTestWorkflow wf = new FailingTestWorkflow();
    doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("failing"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("failing").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);

    executor.run();

    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class),  argThat(new IsTestFailException()));
    verify(listener2).afterFailure(any(ListenerContext.class), argThat(new IsTestFailException()));
    verifyNoMoreInteractions(listener1, listener2);
  }

  static final class IsTestFailException extends ArgumentMatcher<Throwable> {
    @Override
    public boolean matches(Object argument) {
      Throwable t = (RuntimeException) argument;
      t.printStackTrace();
      assertThat(t.getMessage(), equalTo("test-fail"));
      return true;
    }
  }

  @Ignore("not implemented yet")
  @Test
  public void runLaggingWorkflow() {
    // TODO
  }

  @Test
  public void runUnsupportedWorkflow() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    when(workflowDefinitions.getWorkflowDefinition(eq("test"))).thenReturn(null);
    executor.run();
    verify(workflowInstanceDao).updateWorkflowInstance(
        argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 0, true, is(nullValue(DateTime.class)))));
  }

  public static class Pojo {
    public String field;
    public boolean test;
  }

  static List<Object> lastArgs;

  public static class ExecuteTestWorkflow extends
      WorkflowDefinition<ExecuteTestWorkflow.State> {

    protected ExecuteTestWorkflow() {
      super("test", State.start, State.error);
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
      public String getName() {
          return name();
      }

      @Override
      public String getDescription() {
          return name();
      }
    }

    public NextAction start(StateExecution execution) {
      return moveToStateAfter(State.process, getSettings().getErrorTransitionActivation(0), "Process after delay");
    }

    public NextAction process(StateExecution execution, @StateVar("string") String s, @StateVar("int") int i, @StateVar("pojo") Pojo pojo, @StateVar(value="nullPojo", instantiateIfNotExists=true) Pojo pojo2, @StateVar(value="immutablePojo", readOnly=true) Pojo unmodifiablePojo, @StateVar("nullInt") int zero, @StateVar("mutableString") Mutable<String> mutableString) {
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
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), process(WorkflowStateType.normal), error(WorkflowStateType.end),
      processReturnNull(WorkflowStateType.normal), processReturnNullNextState(WorkflowStateType.normal),
      nextStateNoMethod(WorkflowStateType.normal), noMethodEndState(WorkflowStateType.end),
      retryingState(WorkflowStateType.normal), failure(WorkflowStateType.manual);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getName() {
          return name();
      }

      @Override
      public String getDescription() {
          return name();
      }
    }

    public NextAction start(StateExecution execution) {
      throw new RuntimeException("test-fail");
    }

    public NextAction process(StateExecution execution) {
      throw new RuntimeException("test-fail2");
    }

    public NextAction processReturnNull(StateExecution execution) {
      return null;
    }

    public NextAction retryingState(StateExecution execution) {
      return retryAfter(now().plusYears(1), "Retrying");
    }

    public NextAction processReturnNullNextState(StateExecution execution) {
      return moveToState(null, "This should fail");
    }

    public NextAction failure(StateExecution execution) {
      // Return NextAction to verify that backward compatibility is maintained.
      // It should be allowed until nFlow 2.0.0 release, even though the return
      // value is ignored.
      // TODO: remove in 2.0.0
      return moveToState(State.process, "this state transfer will be ignored");
    }

    public NextAction nextStateNoMethod(StateExecution execution) {
      return moveToState(State.noMethodEndState, "Go to end state that has no method");
    }

    public void error(StateExecution execution) {}
  }
}
