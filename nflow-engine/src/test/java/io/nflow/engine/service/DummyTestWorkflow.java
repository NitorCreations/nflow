package io.nflow.engine.service;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;

import java.util.HashMap;
import java.util.Map;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;

@SuppressWarnings("this-escape")
public class DummyTestWorkflow extends WorkflowDefinition {

  public static final String DUMMY_TYPE = "dummy";

  public DummyTestWorkflow() {
    this(new WorkflowSettings.Builder().build());
  }

  public DummyTestWorkflow(WorkflowSettings settings) {
    super(DUMMY_TYPE, BEGIN, DONE, settings);
    permit(BEGIN, DONE, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Finished");
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
