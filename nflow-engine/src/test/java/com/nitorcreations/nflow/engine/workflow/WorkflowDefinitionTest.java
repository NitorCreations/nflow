package com.nitorcreations.nflow.engine.workflow;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.WorkflowDefinitionTest.TestDefinition.TestState;

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

  public static class TestDefinition extends
      WorkflowDefinition<TestDefinition.TestState> {
    public static enum TestState implements WorkflowState {
      start, done, notfound
    }

    public TestDefinition(String type, TestState initialState) {
      super(type, initialState, null);
    }

    public void permitStateTransfer(TestState originState, TestState targetState) {
      permit(originState, targetState);
    }

    public void start(StateExecution execution) {

    }

    public void done(StateExecution execution) {

    }

  }
}
