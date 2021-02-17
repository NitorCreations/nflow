package io.nflow.performance.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.performance.workflow.TestState.DONE;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class NoDelaysWorkflow extends AbstractWorkflowDefinition {

  private static final WorkflowState STATE_1 = new SimpleState("state1", WorkflowStateType.start);
  private static final WorkflowState STATE_2 = new SimpleState("state2");
  private static final WorkflowState STATE_3 = new SimpleState("state3");
  private static final WorkflowState STATE_4 = new SimpleState("state4");
  private static final WorkflowState STATE_5 = new SimpleState("state5");

  public NoDelaysWorkflow() {
    super(NoDelaysWorkflow.class.getSimpleName(), STATE_1, DONE);
    permit(STATE_1, STATE_2);
    permit(STATE_2, STATE_3);
    permit(STATE_3, STATE_4);
    permit(STATE_4, STATE_5);
    permit(STATE_5, DONE);
  }

  public NextAction state1(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(STATE_2, "");
  }

  public NextAction state2(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(STATE_3, "");
  }

  public NextAction state3(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(STATE_4, "");
  }

  public NextAction state4(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(STATE_5, "");
  }

  public NextAction state5(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "");
  }
}
