package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.Mutable;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.StateVar;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;

public class WorkflowExecutorTest extends BaseNflowTest {

  @Mock
  RepositoryService repository;

  ObjectStringMapper objectMapper = new ObjectStringMapper(new ObjectMapper());

  WorkflowExecutor executor;

  @Before
  public void setup() {
    executor = new WorkflowExecutor(1, objectMapper, repository);
  }

  @Test
  public void runWorkflowThroughOneSuccessfulState() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(repository).updateWorkflowInstance(Mockito.argThat(matchesWorkflowInstance(FailingTestWorkflow.State.process, 0, false)),
        Mockito.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, 0)));
  }

  @Test
  public void runWorkflowThroughOneFailedState() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(repository).updateWorkflowInstance(Mockito.argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 1, false)),
        Mockito.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, 0)));
  }

  @Test
  public void runWorkflowToFailureState() {
    WorkflowDefinition<FailingTestWorkflow.State> wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").setRetries(wf.getSettings().getMaxRetries()).build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(repository).updateWorkflowInstance(Mockito.argThat(matchesWorkflowInstance(FailingTestWorkflow.State.failure, 0, false)),
        Mockito.argThat(matchesWorkflowInstanceAction(FailingTestWorkflow.State.start, wf.getSettings().getMaxRetries())));
  }

  @SuppressWarnings("serial")
  @Test
  public void runWorkflowWithParameters() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    Map<String, String> state = new HashMap<String, String>() {{
      put("string", "Str");
      put("int", "42");
      put("pojo", "{\"field\": \"val\", \"test\": true}");
      put("immutablePojo", "{\"field\": \"unmodified\"}");
    }};
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("process").setStateVariables(state).build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    assertThat((String) lastArgs.get(0), is("Str"));
    assertThat((Integer) lastArgs.get(1), is(42));
    assertThat(((Pojo) lastArgs.get(2)).field, is("val modified"));
    assertThat(((Pojo) lastArgs.get(2)).test, is(true));
    assertThat(((Pojo) lastArgs.get(4)).field, is("unmodified ignored"));
    assertThat((Integer) lastArgs.get(5), is(0));
    assertThat(lastArgs.get(6), Matchers.nullValue());
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
    executor = new WorkflowExecutor(1, objectMapper, repository, listener1, listener2);

    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test").setId(Integer.valueOf(1)).setProcessing(true)
        .setState("start").build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
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
    executor = new WorkflowExecutor(1, objectMapper, repository, listener1);

    FailingTestWorkflow wf = new FailingTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("failing"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("failing").setId(Integer.valueOf(1))
        .setProcessing(true).setState("start").build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(listener1).beforeProcessing(any(ListenerContext.class));
    verify(listener1).afterFailure(any(ListenerContext.class),
        Mockito.argThat(new ArgumentMatcher<Exception>() {
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
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    when(repository.getWorkflowDefinition(eq("test"))).thenReturn(null);
    executor.run();
    verify(repository).updateWorkflowInstance(Mockito.argThat(matchesWorkflowInstance(FailingTestWorkflow.State.start, 0, true,
        is(nullValue(DateTime.class)))), Mockito.argThat(is(nullValue(WorkflowInstanceAction.class))));
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
      start, process, done, error;

      @Override
      public WorkflowStateType getType() {
        return WorkflowStateType.normal;
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

    public void done(StateExecution execution) {
    }

    public void error(StateExecution execution) {
    }

  }

  public static class FailingTestWorkflow extends
      WorkflowDefinition<FailingTestWorkflow.State> {

    protected FailingTestWorkflow() {
      super("failing", State.start, State.error);
      permit(State.start, State.process, State.failure);
    }

    public static enum State implements WorkflowState {
      start, process, failure, error;

      @Override
      public WorkflowStateType getType() {
        return WorkflowStateType.normal;
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

    public void failure(StateExecution execution) {
    }

    public void error(StateExecution execution) {
    }

  }

}
