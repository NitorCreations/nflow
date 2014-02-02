package com.nitorcreations.nflow.engine;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

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
  public void runWorkflowThroughOneState() {
    WorkflowDefinition<ExecuteTestWorkflow.State> wf = new ExecuteTestWorkflow();
    Mockito.doReturn(wf).when(repository).getWorkflowDefinition(eq("test"));
    WorkflowInstance instance = constructWorkflowInstanceBuilder()
        .setType("test")
        .setId(Integer.valueOf(1))
        .setCurrentlyProcessing(true)
        .setState("start")
        .build();
    when(repository.getWorkflowInstance(eq(instance.id))).thenReturn(instance);
    executor.run();
    verify(repository).updateWorkflowInstance(Mockito.argThat(new ArgumentMatcher<WorkflowInstance>() {
      @Override
      public boolean matches(Object argument) {
        WorkflowInstance i = (WorkflowInstance)argument;
        assertThat(i.state, equalTo(ExecuteTestWorkflow.State.process.toString()));
        return true;
      }
    }), eq(true));
  }
  
  @Test
  public void runLaggingWorkflow() {
    // TODO
  }
  
  @Test
  public void runUnsupportedWorkflow() {
    // TODO
  }
  
  public static class ExecuteTestWorkflow extends WorkflowDefinition<ExecuteTestWorkflow.State> {
    
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
      execution.setNextActivation(now().plusMillis(getSettings().getErrorBumpedTransitionDelay()));    
    }
    
    public void process(StateExecution execution) {  
      execution.setNextState(State.done);
      execution.setNextActivation(DateTime.now());
    }
    
    public void done(StateExecution execution) {
    }    
    
  }

}
