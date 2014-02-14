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
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

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
    verify(repository).updateWorkflowInstance(
        Mockito.argThat(new ArgumentMatcher<WorkflowInstance>() {
          @Override
          public boolean matches(Object argument) {
            WorkflowInstance i = (WorkflowInstance) argument;
            assertThat(i.state,
                equalTo(ExecuteTestWorkflow.State.process.toString()));
            assertThat(i.retries, is(0));
            assertThat(i.processing, is(false));
            return true;
          }
        }), eq(true));
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
    verify(repository).updateWorkflowInstance(
        Mockito.argThat(new ArgumentMatcher<WorkflowInstance>() {
          @Override
          public boolean matches(Object argument) {
            WorkflowInstance i = (WorkflowInstance) argument;
            assertThat(i.state,
                equalTo(FailingTestWorkflow.State.start.toString()));
            assertThat(i.retries, is(1));
            assertThat(i.processing, is(false));
            return true;
          }
        }), eq(true));
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
      super("test", State.start);
      permit(State.start, State.process);
      permit(State.process, State.done);
    }

    public static enum State implements WorkflowState {
      start, process, done
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

  }

  public static class FailingTestWorkflow extends
      WorkflowDefinition<FailingTestWorkflow.State> {

    protected FailingTestWorkflow() {
      super("failing", State.start);
      permit(State.start, State.process);
    }

    public static enum State implements WorkflowState {
      start, process
    }

    public void start(StateExecution execution) {
      throw new RuntimeException("test-fail");
    }

    public void process(StateExecution execution) {
      throw new RuntimeException("test-fail2");
    }

  }

}
