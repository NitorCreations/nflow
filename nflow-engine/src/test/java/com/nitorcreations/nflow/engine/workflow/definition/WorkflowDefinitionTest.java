package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
  public void errorStateIsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("errorState must not be null");
    new WorkflowDefinition<TestDefinition.TestState>("withoutErrorState", TestState.start, null) {
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
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestState.start);
    verify(execution).setNextState(TestState.error);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(any(DateTime.class));
  }

  @Test
  public void handleRetryMaxRetriesExceededNotHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestState.notfound);
    verify(execution).setNextState(TestState.notfound);
    verify(execution).setNextStateReason(any(String.class));
    verify(execution).setNextActivation(eq((DateTime) null));
  }

  private StateExecutionImpl handleRetryMaxRetriesExceeded(TestState currentState) {
    TestDefinition def = new TestDefinition("x", TestState.start);
    StateExecutionImpl execution = mock(StateExecutionImpl.class);
    when(execution.getRetries()).thenReturn(def.getSettings().maxRetries);
    when(execution.getCurrentStateName()).thenReturn(currentState.name());
    def.handleRetry(execution);
    return execution;
  }

  @Test
  public void succeedsWhenInitialStateMethodExist() {
    new TestDefinition("x", TestState.start);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenInitialStateMethodDoesntExist() {
    new TestDefinition("x", TestState.notfound);
  }

  @Test
  public void succeedWhenPermittingExistingOriginAndTargetState() {
    new TestDefinition("x", TestState.start).permit(TestState.start,
        TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingOriginState() {
    new TestDefinition("x", TestState.start).permit(TestState.notfound,
        TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingTargetState() {
    new TestDefinition("x", TestState.start).permit(TestState.start,
        TestState.notfound);
  }

  @Test
  public void failsWhenFailureStateMethodDoesNotExist() {
    TestDefinition workflow = new TestDefinition("x", TestState.start);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Class '" + workflow.getClass().getName() +
        "' is missing state handling method 'public NextAction notfound(StateExecution execution, ... args)'");

    workflow.permit(TestState.start, TestState.done, TestState.notfound);
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

  public static class TestDefinition extends
      AbstractWorkflowDefinition<TestDefinition.TestState> {
    @Override
    public Set<TestState> getStates() {
      return new HashSet<>(asList(TestState.start, TestState.done, TestState.error));
    }

    public static enum TestState implements WorkflowState {
      start, done, notfound, error;

      @Override
      public WorkflowStateType getType() {
        return normal;
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

    public TestDefinition(String type, TestState initialState) {
      super(type, initialState, TestState.notfound);
      permit(TestState.start, TestState.done, TestState.error);
    }

    public NextAction start(StateExecution execution) { return null; }
    public NextAction done(StateExecution execution) { return null; }
    public NextAction error(StateExecution execution) { return null; }

  }

}
