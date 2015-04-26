package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.DummyTestState> {

  public static enum DummyTestState implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
    start(WorkflowStateType.start), end(WorkflowStateType.end), alternativeStart(WorkflowStateType.start), CreateLoan(WorkflowStateType.start);

    private WorkflowStateType stateType;

    DummyTestState(WorkflowStateType type) {
      stateType = type;
    }

    @Override
    public WorkflowStateType getType() {
      return stateType;
    }

    @Override
    public String getDescription() {
      return null;
    }

  }

  public DummyTestWorkflow() {
    super("dummy", DummyTestState.start, DummyTestState.end);
    permit(DummyTestState.start, DummyTestState.end, DummyTestState.end);
    permit(DummyTestState.alternativeStart, DummyTestState.end);
  }

  public NextAction start(StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }

  public void end(StateExecution execution) {}

  public NextAction alternativeStart(StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }

  public NextAction CreateLoan(StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }
}
