package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;

public class AbstractWorkflowDefinitionTest {

  private final TestWorkflow workflow = new TestWorkflow();
  private final DateTime activation = now().plusDays(1);
  private final StateExecutionImpl execution = mock(StateExecutionImpl.class);

  @Before
  public void setup() {
    setCurrentMillisFixed(DateTime.now().getMillis());
    when(execution.getRetries()).thenReturn(workflow.getSettings().maxRetries);
  }

  @After
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void exceedingMaxRetriesInFailureStateGoesToErrorState() {
    when(execution.getCurrentStateName()).thenReturn(TestWorkflow.State.failed.name());

    workflow.handleRetryAfter(execution, activation);

    verify(execution).setNextState(TestWorkflow.State.error);
    verify(execution).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInNonFailureStateGoesToFailureState() {
    when(execution.getCurrentStateName()).thenReturn(TestWorkflow.State.start.name());

    workflow.handleRetryAfter(execution, activation);

    verify(execution).setNextState(TestWorkflow.State.failed);
    verify(execution).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInNonFailureStateGoesToErrorStateWhenNoFailureStateIsDefined() {
    when(execution.getCurrentStateName()).thenReturn(TestWorkflow.State.startWithoutFailure.name());

    workflow.handleRetryAfter(execution, activation);

    verify(execution).setNextState(TestWorkflow.State.error);
    verify(execution).setNextActivation(now());
  }

  @Test
  public void exceedingMaxRetriesInErrorStateStopsProcessing() {
    when(execution.getCurrentStateName()).thenReturn(TestWorkflow.State.error.name());

    workflow.handleRetryAfter(execution, activation);

    verify(execution).setNextState(TestWorkflow.State.error);
    verify(execution).setNextActivation(null);
  }

  @Test
  public void handleRetryAfterSetsActivationWhenMaxRetriesIsNotExceeded() {
    when(execution.getCurrentStateName()).thenReturn(TestWorkflow.State.start.name());
    when(execution.getRetries()).thenReturn(0);

    workflow.handleRetryAfter(execution, activation);

    verify(execution, never()).setNextState(any(TestWorkflow.State.class));
    verify(execution).setNextActivation(activation);
  }

  static class TestWorkflow extends WorkflowDefinition<TestWorkflow.State> {

    protected TestWorkflow() {
      super("test", State.start, State.error);
      permit(State.start, State.done, State.failed);
      permit(State.startWithoutFailure, State.done);
    }

    public static enum State implements WorkflowState {
      start(WorkflowStateType.start), startWithoutFailure(WorkflowStateType.start), done(WorkflowStateType.end), failed(WorkflowStateType.end), error(WorkflowStateType.manual);

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
      return stopInState(State.done, "Done");
    }

    public NextAction startWithoutFailure(StateExecution execution) {
      return stopInState(State.done, "Done");
    }
  }
}
