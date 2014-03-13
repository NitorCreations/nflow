package com.nitorcreations.nflow.engine.workflow;

import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.normal;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.WorkflowDefinitionTest.TestDefinition.TestState;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinitionTest.TestDefinition2.TestState2;

public class WorkflowDefinitionTest {

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
  public void allowedTranstionsCanContainMultipleTargetStates(){
    WorkflowDefinition<?> def = new TestDefinition2("y", TestState2.start);
    assertEquals(asList(TestState2.done.name(),
        TestState2.state1.name(), TestState2.state2.name()), def.getAllowedTransitions().get(TestState2.start.name()));
    assertEquals(TestState2.notfound.name(), def.getFailureTransitions().get(TestState2.start.name()));
  }

  public static class TestDefinition extends
      WorkflowDefinition<TestDefinition.TestState> {
    public static enum TestState implements WorkflowState {
      start, done, notfound;

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
    }

    public void permitStateTransfer(TestState originState, TestState targetState) {
      permit(originState, targetState);
    }

    public void start(StateExecution execution) {
    }

    public void done(StateExecution execution) {
    }

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
