package io.nflow.engine.service;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;

import java.util.HashMap;
import java.util.Map;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends AbstractWorkflowDefinition<WorkflowState> {

  private static final WorkflowState ALTERNATIVE_START = new SimpleState("alternativeStart", WorkflowStateType.start);
  private static final WorkflowState CREATE_LOAN = new SimpleState("CreateLoan", WorkflowStateType.start);

  public DummyTestWorkflow() {
    this(new WorkflowSettings.Builder().build());
  }

  public DummyTestWorkflow(WorkflowSettings settings) {
    super("dummy", BEGIN, DONE, settings);
    permit(BEGIN, DONE, DONE);
    permit(ALTERNATIVE_START, DONE);
    registerState(CREATE_LOAN);
  }

  public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Finished");
  }

  public void end(@SuppressWarnings("unused") StateExecution execution) {
  }

  public NextAction alternativeStart(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Finished");
  }

  public NextAction CreateLoan(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Finished");
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    Map<Integer, String> signals = new HashMap<>();
    signals.put(2, "number two");
    signals.put(1, "number one");
    return signals;
  }
}
