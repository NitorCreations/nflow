package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionTest.TestDefinition.TestState;

public class WorkflowDefinitionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void initialStateIsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("initialState must not be null");
    new WorkflowDefinition<TestDefinition.TestState>("withoutInitialState", null, TestState.error) {
    };
  }

  @Test
  public void initialStateMustBeStartState() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("initialState must be a start state");
    new WorkflowDefinition<TestDefinition.TestState>("nonStartInitialState", TestState.done, TestState.error) {
    };
  }

  @Test
  public void errorStateIsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("errorState must not be null");
    new WorkflowDefinition<TestDefinition.TestState>("withoutErrorState", TestState.start1, null) {
    };
  }

  @Test
  public void errorStateMustBeFinalState() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("errorState must be a final state");
    new WorkflowDefinition<TestDefinition.TestState>("nonFinalErrorState", TestState.start1, TestState.start1) {
    };
  }

  @Test
  public void getStatesWorks() {
    TestDefinitionWithStateTypes def = new TestDefinitionWithStateTypes("x", TestDefinitionWithStateTypes.State.initial);
    assertThat(def.getStates(),
        containsInAnyOrder(TestDefinitionWithStateTypes.State.initial, TestDefinitionWithStateTypes.State.done,
            TestDefinitionWithStateTypes.State.state1, TestDefinitionWithStateTypes.State.state2, TestDefinitionWithStateTypes.State.error));
  }

  @Test
  public void handleRetryMaxRetriesExceededHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestState.start1);
    verify(execution).setNextState(TestState.failed);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(any(DateTime.class));
  }

  @Test
  public void handleRetryMaxRetriesExceededNotHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestState.start2);
    verify(execution).setNextState(TestState.error);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(any(DateTime.class));
  }

  private StateExecutionImpl handleRetryMaxRetriesExceeded(TestState currentState) {
    TestDefinition def = new TestDefinition("x", currentState);
    StateExecutionImpl execution = mock(StateExecutionImpl.class);
    when(execution.getRetries()).thenReturn(def.getSettings().maxRetries);
    when(execution.getCurrentStateName()).thenReturn(currentState.name());
    def.handleRetry(execution);
    return execution;
  }

  @Test
  public void succeedsWhenInitialStateMethodExist() {
    new TestDefinition("x", TestState.start1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenInitialStateMethodDoesntExist() {
    new TestDefinition("x", TestState.notfound);
  }

  @Test
  public void succeedWhenPermittingExistingOriginAndTargetState() {
    new TestDefinition("x", TestState.start1).permit(TestState.start1,
        TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingOriginState() {
    new TestDefinition("x", TestState.start1).permit(TestState.notfound,
        TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingTargetState() {
    new TestDefinition("x", TestState.start1).permit(TestState.start1,
        TestState.notfound);
  }

  @Test
  public void failsWhenFailureStateMethodDoesNotExist() {
    TestDefinition workflow = new TestDefinition("x", TestState.start1);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Class '" + workflow.getClass().getName() +
        "' is missing non-final state handling method 'public NextAction notfound(StateExecution execution, ... args)'");

    workflow.permit(TestState.start1, TestState.done, TestState.notfound);
  }

  @Test
  public void allowedTranstionsCanContainMultipleTargetStates(){
    WorkflowDefinition<?> def = new TestDefinitionWithStateTypes("y", TestDefinitionWithStateTypes.State.initial);
    assertEquals(asList(TestDefinitionWithStateTypes.State.done.name(),
        TestDefinitionWithStateTypes.State.state1.name(), TestDefinitionWithStateTypes.State.state2.name()),
        def.getAllowedTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
    assertEquals(TestDefinitionWithStateTypes.State.error, def.getFailureTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
  }

  @Test
  public void isStartStateWorks() {
    WorkflowDefinition<?> workflow = new TestDefinitionWithStateTypes("y", TestDefinitionWithStateTypes.State.initial);

    assertThat(workflow.isStartState("initial"), equalTo(true));
    assertThat(workflow.isStartState("state1"), equalTo(false));
    assertThat(workflow.isStartState("state2"), equalTo(false));
    assertThat(workflow.isStartState("error"), equalTo(false));
    assertThat(workflow.isStartState("done"), equalTo(false));
  }

  public static class TestDefinition extends AbstractWorkflowDefinition<TestDefinition.TestState> {

    @Override
    public Set<TestState> getStates() {
      return new HashSet<>(asList(TestState.start1, TestState.start2, TestState.done, TestState.error, TestState.failed));
    }

    public static enum TestState implements WorkflowState {
      start1(start), start2(start), done(end), notfound(normal), failed(end), error(end);

      private final WorkflowStateType type;

      private TestState(WorkflowStateType type) {
        this.type = type;
      }

      @Override
      public WorkflowStateType getType() {
        return type;
      }

      @Override
      public String getDescription() {
        return name();
      }
    }

    public TestDefinition(String type, TestState initialState) {
      super(type, initialState, TestState.error);
      permit(TestState.start1, TestState.done, TestState.failed);
      permit(TestState.start2, TestState.done);
    }

    public NextAction start1(StateExecution execution) { return null; }
    public NextAction start2(StateExecution execution) { return null; }
    public void done(StateExecution execution) {}
    public void failed(StateExecution execution) {}
    public void error(StateExecution execution) {}
  }
}
