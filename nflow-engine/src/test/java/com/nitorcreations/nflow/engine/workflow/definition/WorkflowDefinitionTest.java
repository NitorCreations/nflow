package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    verify(execution).setNextState(eq(TestState.error.name()), any(String.class), any(DateTime.class));
  }

  @Test
  public void handleRetryMaxRetriesExceededNotHaveFailureState() {
    StateExecutionImpl execution = handleRetryMaxRetriesExceeded(TestState.notfound);
    verify(execution).setNextState(eq(TestState.notfound), any(String.class), isNull(DateTime.class));
  }

  private StateExecutionImpl handleRetryMaxRetriesExceeded(TestState currentState) {
    TestDefinition def = new TestDefinition("x", TestState.start);
    StateExecutionImpl execution = mock(StateExecutionImpl.class);
    when(execution.getRetries()).thenReturn(def.getSettings().getMaxRetries());
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
    thrown.expectMessage("Class " + workflow.getClass().getName() +
        " is missing state handling method notfound(StateExecution execution, ... args)");

    workflow.permit(TestState.start, TestState.done, TestState.notfound);
  }

  @Test
  public void allowedTranstionsCanContainMultipleTargetStates(){
    WorkflowDefinition<?> def = new TestDefinitionWithStateTypes("y", TestDefinitionWithStateTypes.State.initial);
    assertEquals(asList(TestDefinitionWithStateTypes.State.done.name(),
        TestDefinitionWithStateTypes.State.state1.name(), TestDefinitionWithStateTypes.State.state2.name()),
        def.getAllowedTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
    assertEquals(TestDefinitionWithStateTypes.State.error.name(), def.getFailureTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
  }

  public static class TestDefinition extends
      AbstractWorkflowDefinition<TestDefinition.TestState> {
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

    public void start(StateExecution execution) { }
    public void done(StateExecution execution) { }
    public void error(StateExecution execution) { }

    @Override
    public Set<TestState> getStates() {
      // TODO Auto-generated method stub
      return null;
    }

  }

}
