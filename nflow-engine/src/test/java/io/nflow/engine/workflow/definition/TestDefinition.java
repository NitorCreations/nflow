package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.TestState.DONE;

import io.nflow.engine.workflow.curated.State;

public class TestDefinition extends WorkflowDefinition {

  public static final WorkflowState START_1 = new State("start1", WorkflowStateType.start);
  public static final WorkflowState START_2 = new State("start2", WorkflowStateType.start);
  public static final WorkflowState FAILED = new State("failed", WorkflowStateType.end);
  public static final WorkflowState ERROR = new State("error", WorkflowStateType.end);

  public TestDefinition(String type, WorkflowState initialState) {
    super(type, initialState, ERROR);
    permit(START_1, DONE, FAILED);
    permit(START_2, DONE);
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
