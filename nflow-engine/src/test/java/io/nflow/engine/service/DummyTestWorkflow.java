package io.nflow.engine.service;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;

import java.util.HashMap;
import java.util.Map;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.DummyTestState> {

  public static enum DummyTestState implements io.nflow.engine.workflow.definition.WorkflowState {
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

  public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }

  public void end(@SuppressWarnings("unused") StateExecution execution) {
  }

  public NextAction alternativeStart(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }

  public NextAction CreateLoan(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DummyTestState.end, "Finished");
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    Map<Integer, String> signals = new HashMap<>();
    signals.put(2, "number two");
    signals.put(1, "number one");
    return signals;
  }

}
