package io.nflow.performance.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.performance.workflow.TestState.BEGIN;
import static io.nflow.performance.workflow.TestState.DONE;
import static io.nflow.performance.workflow.TestState.ERROR;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.joda.time.DateTime.now;
import static org.joda.time.Duration.standardSeconds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;

/**
 * Deterministic workflow that executes quickly.
 */
public class ConstantWorkflow extends WorkflowDefinition {
  private static final Logger logger = LoggerFactory.getLogger(ConstantWorkflow.class);
  private static final String KEY = "retries";

  public static final WorkflowState QUICK_STATE = new State("quickState", "This executes fast then goes to retryTwice");
  public static final WorkflowState RETRY_TWICE_STATE = new State("retryTwiceState",
      "Retries twice and goes then goes to scheduleState");
  public static final WorkflowState SCHEDULE_STATE = new State("scheduleState", "Goes to slowState, in 3 sec");
  public static final WorkflowState SLOW_STATE = new State("slowState", "This executes bit slower. Goes to end");

  public ConstantWorkflow() {
    super(ConstantWorkflow.class.getSimpleName(), BEGIN, ERROR,
        new WorkflowSettings.Builder().setMaxErrorTransitionDelay(standardSeconds(5)).build());
    permit(BEGIN, QUICK_STATE);
    permit(QUICK_STATE, RETRY_TWICE_STATE);
    permit(RETRY_TWICE_STATE, SCHEDULE_STATE);
    permit(SCHEDULE_STATE, SLOW_STATE);
    permit(SLOW_STATE, DONE);
  }

  public NextAction start(StateExecution execution) {
    // nothing here
    execution.setVariable(KEY, 0);
    return moveToState(QUICK_STATE, "Time for quickness");
  }

  public NextAction quickState(@SuppressWarnings("unused") StateExecution execution) {
    try {
      MILLISECONDS.sleep(10);
    } catch (@SuppressWarnings("unused") InterruptedException e) {
      // ignore
    }
    return moveToState(RETRY_TWICE_STATE, "Go do some retries");
  }

  public NextAction retryTwiceState(StateExecution execution) {
    // Retries once and goes then goes to scheduleState
    Integer retryCount = execution.getVariable(KEY, Integer.class);
    retryCount++;
    execution.setVariable(KEY, retryCount);
    if (retryCount > 2) {
      logger.info("Retry count {}. Go to next state", retryCount);
      return moveToState(SCHEDULE_STATE, "Schedule some action");

    }
    throw new RuntimeException("Retry count " + retryCount + ". Retrying");
  }

  public NextAction scheduleState(@SuppressWarnings("unused") StateExecution execution) {
    return moveToStateAfter(SLOW_STATE, now().plusSeconds(3), "Schedule some action");
  }

  public NextAction slowState(@SuppressWarnings("unused") StateExecution execution) {
    try {
      MILLISECONDS.sleep(500);
    } catch (@SuppressWarnings("unused") InterruptedException e) {
      // ignore
    }
    return stopInState(DONE, "Goto end");
  }

  public void error(@SuppressWarnings("unused") StateExecution execution) {
    logger.error("should not happen");
  }
}
