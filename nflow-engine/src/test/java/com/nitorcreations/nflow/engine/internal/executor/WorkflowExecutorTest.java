package com.nitorcreations.nflow.engine.internal.executor;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.definition.Mutable;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class WorkflowExecutorTest extends BaseNflowTest {

  @Mock
  WorkflowDefinitionService workflowDefinitions;

  @Mock
  WorkflowInstanceService workflowInstances;

  @Captor
  ArgumentCaptor<WorkflowInstance> update;

  ObjectStringMapper objectMapper = new ObjectStringMapper(new ObjectMapper());

  WorkflowExecutor executor;

  @Before
  public void setup() {
    executor = new WorkflowExecutor(1, objectMapper, workflowDefinitions, workflowInstances);
  }

  @Test
  public void runWorkflowThroughOneSuccessfulState() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(ExecuteTestWorkflow.State.process, 0, false)),
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
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 1, false)),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, 0)));
  }

  @Test
  public void runWorkflowToThroughFailureState() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").setRetries(wf.getSettings().getMaxRetries()).build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(FailingTestWorkflow.State.failure, 0, false)),
        argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, wf.getSettings().getMaxRetries())));
  }

  @Test
  public void nextActivationClearedWhenMissingHandlerMethod() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("failure").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, false,
        is(nullValue(DateTime.class)))), argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.failure, 0)));
  }

  @Test
  public void errorStateWhenNoNextStateDefined() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("processNoNextState").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(FailingTestWorkflow.State.error, 0, false,
        is(nullValue(DateTime.class)))), argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.processNoNextState, 0)));
  }

  @SuppressWarnings("serial")
  @Test
  public void runWorkflowWithParameters() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
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
    doNothing().when(workflowInstances).updateWorkflowInstance(update.capture(), argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.process, 0)));
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
    WorkflowExecutorListener listener1 = Mockito
        .mock(WorkflowExecutorListener.class);
    WorkflowExecutorListener listener2 = Mockito
        .mock(WorkflowExecutorListener.class);
    executor = new WorkflowExecutor(1, objectMapper, workflowDefinitions, workflowInstances, listener1, listener2);

    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener2).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterProcessing(any(ListenerContext.class));
    verify(listener2).afterProcessing(any(ListenerContext.class));

    verify(listener1, times(0)).afterFailure(any(ListenerContext.class),
        any(Throwable.class));
    verify(listener2, times(0)).afterFailure(any(ListenerContext.class),
        any(Throwable.class));
  }

  @Test
  public void failureListenersAreExecutedAfterFailure() {
    WorkflowExecutorListener listener1 = Mockito
        .mock(WorkflowExecutorListener.class);
    executor = new WorkflowExecutor(1, objectMapper, workflowDefinitions, workflowInstances, listener1);

    FailingTestWorkflow wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(workflowDefinitions).getWorkflowDefinition(eq("failing"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("failing").setId(Integer.valueOf(1))
        .setProcessing(true).setState("start").build();
    when(workflowInstances.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class),
        argThat(new ArgumentMatcher<Exception>() {
          @Override
          public boolean matches(Object argument) {
            Exception ex = (RuntimeException) argument;
            ex.printStackTrace();
            assertThat(ex.getMessage(), equalTo("test-fail"));
            return true;
          }
        }));

    verify(listener1, times(0)).afterProcessing(any(ListenerContext.class));
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
    verify(workflowInstances).updateWorkflowInstance(argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 0, true,
        is(nullValue(DateTime.class)))), argThat(is(nullValue(WorkflowInstanceAction.class))));
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

    public void start(StateExecution execution) {
      execution.setNextState(State.process);
      execution.setNextActivation(now().plusMillis(
          getSettings().getErrorTransitionDelay()));
    }

    public void process(StateExecution execution, @StateVar("string") String s, @StateVar("int") int i, @StateVar("pojo") Pojo pojo, @StateVar(value="nullPojo", instantiateNull=true) Pojo pojo2, @StateVar(value="immutablePojo", readOnly=true) Pojo unmodifiablePojo, @StateVar("nullInt") int zero, @StateVar("mutableString") Mutable<String> mutableString) {
      execution.setNextState(State.done);
      execution.setNextActivation(DateTime.now());
      Pojo pojo1 = execution.getVariable("pojo", Pojo.class);
      assertThat(pojo.field, is(pojo1.field));
      assertThat(pojo.test, is(pojo1.test));
      lastArgs = asList(s, i, pojo, pojo2, unmodifiablePojo, zero, mutableString.val);
      pojo.field += " modified";
      pojo2.field = "magical instance";
      unmodifiablePojo.field += " ignored";
      mutableString.val = "mutated";
      execution.setVariable("hello", new int[]{1,2,3});
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
      processNoNextState(WorkflowStateType.normal), failure(WorkflowStateType.manual);

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

    public void start(StateExecution execution) {
      throw new RuntimeException("test-fail");
    }

    public void process(StateExecution execution) {
      throw new RuntimeException("test-fail2");
    }

    public void processNoNextState(StateExecution execution) {
    }

    public void failure(StateExecution execution) {
      execution.setNextState(State.error, "Go to error state", now());
    }

  }

}
