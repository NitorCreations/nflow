package com.nitorcreations.nflow.performance.workflow;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class NoDelaysWorkflow extends WorkflowDefinition<NoDelaysWorkflow.QuickState> {
  public static enum QuickState implements WorkflowState {

    state1(WorkflowStateType.start, "state1"), state2("state2"), state3("state3"), state4("state4"), state5("state5"), end(
        WorkflowStateType.end, "end");

    private final WorkflowStateType type;
    private final String description;

    private QuickState(String description) {
      this(WorkflowStateType.normal, description);
    }

    private QuickState(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public NoDelaysWorkflow() {
    super(NoDelaysWorkflow.class.getSimpleName(), QuickState.state1, QuickState.end);
    permit(QuickState.state1, QuickState.state2);
    permit(QuickState.state2, QuickState.state3);
    permit(QuickState.state3, QuickState.state4);
    permit(QuickState.state4, QuickState.state5);
    permit(QuickState.state5, QuickState.end);
  }

  public NextAction state1(StateExecution execution) {
    return NextAction.moveToState(QuickState.state2, "");
  }

  public NextAction state2(StateExecution execution) {
    return NextAction.moveToState(QuickState.state3, "");
  }

  public NextAction state3(StateExecution execution) {
    return NextAction.moveToState(QuickState.state4, "");
  }

  public NextAction state4(StateExecution execution) {
    return NextAction.moveToState(QuickState.state5, "");
  }

  public NextAction state5(StateExecution execution) {
    return NextAction.stopInState(QuickState.end, "");
  }

}
