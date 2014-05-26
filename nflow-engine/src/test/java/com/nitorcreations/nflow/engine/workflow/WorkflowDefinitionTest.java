package com.nitorcreations.nflow.engine.workflow;

import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.normal;
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

import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinitionTest.TestDefinition.TestState;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinitionTest.TestDefinition2.TestState2;

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
    TestDefinition def = new TestDefinition("x", TestState.start);
    assertThat(def.getStates(),
        containsInAnyOrder((WorkflowState)TestState.start, (WorkflowState)TestState.done,
            (WorkflowState)TestState.notfound, (WorkflowState)TestState.error));
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
        " is missing state handling method notfound(StateExecution execution)");

    workflow.permit(TestState.start, TestState.done, TestState.notfound);
  }

  @Test
  public void allowedTranstionsCanContainMultipleTargetStates(){
    WorkflowDefinition<?> def = new TestDefinition2("y", TestState2.start);
    assertEquals(asList(TestState2.done.name(),
        TestState2.state1.name(), TestState2.state2.name()), def.getAllowedTransitions().get(TestState2.start.name()));
    assertEquals(TestState2.notfound.name(), def.getFailureTransitions().get(TestState2.start.name()));
  }

  public static class TestDefinition extends
      WorkflowDefinition<TestDefinition.TestState> {
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

  }

  public static class TestDefinition2 extends
      WorkflowDefinition<TestDefinition2.TestState2> {
    public static enum TestState2 implements WorkflowState {
      start, done, state1, state2, notfound;

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

    public TestDefinition2(String type, TestState2 initialState) {
      super(type, initialState, TestState2.notfound);
      permit(TestState2.start, TestState2.done, TestState2.notfound);
      permit(TestState2.start, TestState2.state1);
      permit(TestState2.start, TestState2.state2);
      permit(TestState2.state1, TestState2.state2);
      permit(TestState2.state2, TestState2.done);
    }

    public void start(StateExecution execution) {
    }

    public void state1(StateExecution execution) {
    }

    public void state2(StateExecution execution) {
    }

    public void done(StateExecution execution) {
    }

    public void notfound(StateExecution execution) {
    }

  }

}
