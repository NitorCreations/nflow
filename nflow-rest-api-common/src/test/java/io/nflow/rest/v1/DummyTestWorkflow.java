package io.nflow.rest.v1;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static org.joda.time.Period.days;

import java.util.HashMap;
import java.util.Map;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends AbstractWorkflowDefinition {

  public static final WorkflowState START = new SimpleState("start", WorkflowStateType.start);
  public static final WorkflowState ERROR = new SimpleState("error", WorkflowStateType.manual);
  public static final WorkflowState END = new SimpleState("end", WorkflowStateType.end);

  public DummyTestWorkflow() {
    super("dummy", START, ERROR, new WorkflowSettings.Builder().setMinErrorTransitionDelay(300).setMaxErrorTransitionDelay(1000)
        .setShortTransitionDelay(200).setImmediateTransitionDelay(100).setMaxRetries(10).setHistoryDeletableAfter(days(3))
        .build());
    permit(START, END, ERROR);
    permit(START, ERROR);
    permit(ERROR, END);
  }

  public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(END, "Go to end state");
  }

  public void error(@SuppressWarnings("unused") StateExecution execution) {
  }

  public void end(@SuppressWarnings("unused") StateExecution execution) {
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    Map<Integer, String> signals = new HashMap<>();
    signals.put(2, "two");
    signals.put(1, "one");
    return signals;
  }
}
