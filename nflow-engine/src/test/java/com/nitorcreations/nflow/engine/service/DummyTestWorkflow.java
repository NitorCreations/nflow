package com.nitorcreations.nflow.engine.service;

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
    public String getName() {
      return toString();
    }

    @Override
    public String getDescription() {
      return null;
    }

  }

  protected DummyTestWorkflow() {
    super("dummy", DummyTestState.start, DummyTestState.end);
  }

  public void start(StateExecution execution) {
    execution.setNextState(DummyTestState.end);
  }

  public void end(StateExecution execution) {
    execution.setNextState(DummyTestState.end);
  }

  public void alternativeStart(StateExecution execution) {
    execution.setNextState(DummyTestState.end);
  }
  public void CreateLoan(StateExecution execution) {
    execution.setNextState(DummyTestState.end);
  }

}
