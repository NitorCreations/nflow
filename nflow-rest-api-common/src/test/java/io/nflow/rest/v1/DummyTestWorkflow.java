package io.nflow.rest.v1;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.rest.v1.TestState.BEGIN;
import static io.nflow.rest.v1.TestState.DONE;
import static io.nflow.rest.v1.TestState.ERROR;
import static org.joda.time.Duration.millis;
import static org.joda.time.Period.days;

import java.util.HashMap;
import java.util.Map;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;

public class DummyTestWorkflow extends AbstractWorkflowDefinition {

  public DummyTestWorkflow() {
    super("dummy", BEGIN, ERROR,
        new WorkflowSettings.Builder().setMinErrorTransitionDelay(millis(300)).setMaxErrorTransitionDelay(millis(1000))
            .setShortTransitionDelay(millis(200)).setMaxRetries(10).setHistoryDeletableAfter(days(3)).build());
    permit(BEGIN, DONE, ERROR);
    permit(BEGIN, ERROR);
    permit(ERROR, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DONE, "Go to end state");
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    Map<Integer, String> signals = new HashMap<>();
    signals.put(2, "two");
    signals.put(1, "one");
    return signals;
  }
}
