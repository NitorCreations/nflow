package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;
import static io.nflow.engine.workflow.definition.TestState.ERROR;

import io.nflow.engine.workflow.curated.SimpleState;

public class TestDefinitionWithStateTypes extends AbstractWorkflowDefinition<WorkflowState> {

  static final WorkflowState STATE_1 = new SimpleState("state1");
  static final WorkflowState STATE_2 = new SimpleState("state2");

  public TestDefinitionWithStateTypes(String type, WorkflowState initialState) {
    super(type, initialState, ERROR);
    permit(BEGIN, DONE, ERROR);
    permit(BEGIN, STATE_1);
    permit(BEGIN, STATE_2);
    permit(STATE_1, STATE_2);
    permit(STATE_2, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return null;
  }

  public NextAction state1(@SuppressWarnings("unused") StateExecution execution,
      @SuppressWarnings("unused") @StateVar("arg") String param) {
    return null;
  }

  public NextAction state2(@SuppressWarnings("unused") StateExecution execution) {
    return null;
  }
}
