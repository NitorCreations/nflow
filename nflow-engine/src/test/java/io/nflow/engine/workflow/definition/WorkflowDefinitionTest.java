package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WorkflowDefinitionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void initialStateIsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("initialState must not be null");
    new WorkflowDefinition<TestDefinition.TestState>("withoutInitialState", null, TestDefinition.TestState.error) {
    };
  }

  @Test
  public void initialStateMustBeStartState() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("initialState must be a start state");
    new WorkflowDefinition<TestDefinition.TestState>("nonStartInitialState", TestDefinition.TestState.done,
        TestDefinition.TestState.error) {
    };
  }

  @Test
  public void errorStateIsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("errorState must not be null");
    new WorkflowDefinition<TestDefinition.TestState>("withoutErrorState", TestDefinition.TestState.start1, null) {
    };
  }

  @Test
  public void errorStateMustBeFinalState() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("errorState must be a final state");
    new WorkflowDefinition<TestDefinition.TestState>("nonFinalErrorState", TestDefinition.TestState.start1,
        TestDefinition.TestState.start1) {
    };
  }

  @Test
  public void getStatesWorks() {
    TestDefinitionWithStateTypes def = new TestDefinitionWithStateTypes("x", TestDefinitionWithStateTypes.State.initial);
    assertThat(def.getStates(),
        containsInAnyOrder(TestDefinitionWithStateTypes.State.initial, TestDefinitionWithStateTypes.State.done,
            TestDefinitionWithStateTypes.State.state1, TestDefinitionWithStateTypes.State.state2,
            TestDefinitionWithStateTypes.State.error));
  }

  @Test
  public void succeedsWhenInitialStateMethodExist() {
    new TestDefinition("x", TestDefinition.TestState.start1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenInitialStateMethodDoesntExist() {
    new TestDefinition("x", TestDefinition.TestState.notfound);
  }

  @Test
  public void succeedWhenPermittingExistingOriginAndTargetState() {
    new TestDefinition("x", TestDefinition.TestState.start1).permit(TestDefinition.TestState.start1,
        TestDefinition.TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingOriginState() {
    new TestDefinition("x", TestDefinition.TestState.start1).permit(TestDefinition.TestState.notfound,
        TestDefinition.TestState.done);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenPermittingNonExistingTargetState() {
    new TestDefinition("x", TestDefinition.TestState.start1).permit(TestDefinition.TestState.start1,
        TestDefinition.TestState.notfound);
  }

  @Test
  public void failsWhenFailureStateMethodDoesNotExist() {
    TestDefinition workflow = new TestDefinition("x", TestDefinition.TestState.start1);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Class '" + workflow.getClass().getName()
        + "' is missing non-final state handling method 'public NextAction notfound(StateExecution execution, ... args)'");

    workflow.permit(TestDefinition.TestState.start1, TestDefinition.TestState.done, TestDefinition.TestState.notfound);
  }

  @Test
  public void allowedTranstionsCanContainMultipleTargetStates() {
    WorkflowDefinition<?> def = new TestDefinitionWithStateTypes("y", TestDefinitionWithStateTypes.State.initial);
    assertEquals(asList(TestDefinitionWithStateTypes.State.done.name(), TestDefinitionWithStateTypes.State.state1.name(),
        TestDefinitionWithStateTypes.State.state2.name()),
        def.getAllowedTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
    assertEquals(TestDefinitionWithStateTypes.State.error,
        def.getFailureTransitions().get(TestDefinitionWithStateTypes.State.initial.name()));
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

    public NextAction start1(@SuppressWarnings("unused") StateExecution execution) {
      return null;
    }

    public NextAction start2(@SuppressWarnings("unused") StateExecution execution) {
      return null;
    }

    public void done(@SuppressWarnings("unused") StateExecution execution) {
    }

    public void failed(@SuppressWarnings("unused") StateExecution execution) {
    }

    public void error(@SuppressWarnings("unused") StateExecution execution) {
    }
  }
}
