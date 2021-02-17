package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.PROCESS;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowState;

public class SlowWorkflow extends AbstractWorkflowDefinition {

  public static final String SLOW_WORKFLOW_TYPE = "slowWorkflow";
  public static final int SIGNAL_INTERRUPT = 1;

  public static final WorkflowState INTERRUPTED = new SimpleState("interrupted", end);

  public SlowWorkflow() {
    super(SLOW_WORKFLOW_TYPE, BEGIN, ERROR);
    setDescription("Workflow for testing a state that has a long execution time");
    permit(BEGIN, PROCESS);
    permit(PROCESS, INTERRUPTED);
    permit(PROCESS, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(PROCESS, "Go to process state");
  }

  public NextAction process(StateExecution execution) throws InterruptedException {
    for (int i = 0; i < 50; i++) {
      MILLISECONDS.sleep(200);
      if (execution.getSignal().isPresent()) {
        Integer signal = execution.getSignal().get();
        execution.setSignal(empty(), "Clearing signal from process state");
        return moveToState(INTERRUPTED, "Interrupted with signal " + signal + ", moving to interrupted state");
      }
    }
    return moveToState(DONE, "Go to done state");
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    return singletonMap(SIGNAL_INTERRUPT, "Interrupted");
  }
}
