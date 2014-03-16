package com.nitorcreations.nflow.engine;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.nitorcreations.nflow.engine.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class WorkflowExecutorTest extends BaseNflowTest {

  @Mock
  RepositoryService repository;

  WorkflowExecutor executor;

  @Before
  public void setup() {
    executor = new WorkflowExecutor(1, repository);
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

  private ArgumentMatcher<WorkflowInstance> matchesWorkflowInstance(final WorkflowState state,
      final int retries, final boolean isProcessing) {
    return new ArgumentMatcher<WorkflowInstance>() {
      @Override
      public boolean matches(Object argument) {
        WorkflowInstance i = (WorkflowInstance) argument;
        assertThat(i.state, equalTo(state.name()));
        assertThat(i.retries, is(retries));
        assertThat(i.processing, is(isProcessing));
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
    executor = new WorkflowExecutor(1, repository, listener1, listener2);

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
    executor = new WorkflowExecutor(1, repository, listener1);

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

  @Ignore("not implemented yet")
  @Test
  public void runUnsupportedWorkflow() {
    // TODO
  }

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

    public void process(StateExecution execution) {
      execution.setNextState(State.done);
      execution.setNextActivation(DateTime.now());
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
