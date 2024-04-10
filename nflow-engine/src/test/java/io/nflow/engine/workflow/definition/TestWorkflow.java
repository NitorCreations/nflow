package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;
import static io.nflow.engine.workflow.definition.TestState.ERROR;

import io.nflow.engine.workflow.curated.State;

public class TestWorkflow extends WorkflowDefinition {

  public static final WorkflowState START_WITHOUT_FAILURE = new State("startWithoutFailure", WorkflowStateType.start);
  public static final WorkflowState FAILED = new State("failed", WorkflowStateType.end);

  @SuppressWarnings("this-escape")
  public TestWorkflow() {
    super("test", BEGIN, ERROR);
    permit(BEGIN, DONE, FAILED);
    permit(START_WITHOUT_FAILURE, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }

  public NextAction startWithoutFailure(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }
}