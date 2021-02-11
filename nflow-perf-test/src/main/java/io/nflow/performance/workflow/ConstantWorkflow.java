package io.nflow.performance.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static org.joda.time.DateTime.now;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

/**
 * Deterministic workflow that executes quickly.
 */
public class ConstantWorkflow extends AbstractWorkflowDefinition<WorkflowState> {
  private static final Logger logger = LoggerFactory.getLogger(ConstantWorkflow.class);
  private static final String KEY = "retries";

  public static final WorkflowState START = new SimpleState("start", WorkflowStateType.start);
  public static final WorkflowState QUICK_STATE = new SimpleState("quickState", "This executes fast then goes to retryTwice");
  public static final WorkflowState RETRY_TWICE_STATE = new SimpleState("retryTwiceState",
      "Retries twice and goes then goes to scheduleState");
  public static final WorkflowState SCHEDULE_STATE = new SimpleState("scheduleState", "Goes to slowState, in 3 sec");
  public static final WorkflowState SLOW_STATE = new SimpleState("slowState", "This executes bit slower. Goes to end");
  public static final WorkflowState END = new SimpleState("end", WorkflowStateType.end);
  public static final WorkflowState ERROR = new SimpleState("error", WorkflowStateType.end, "Error. Should not be used.");

  public ConstantWorkflow() {
    super(ConstantWorkflow.class.getSimpleName(), START, ERROR,
        new WorkflowSettings.Builder().setMaxErrorTransitionDelay(5000).build());
    permit(START, QUICK_STATE);
    permit(QUICK_STATE, RETRY_TWICE_STATE);
    permit(RETRY_TWICE_STATE, SCHEDULE_STATE);
    permit(SCHEDULE_STATE, SLOW_STATE);
    permit(SLOW_STATE, END);
  }

  public NextAction start(StateExecution execution) {
    // nothing here
    execution.setVariable(KEY, 0);
    return moveToState(QUICK_STATE, "Time for quickness");
  }

  public NextAction quickState(@SuppressWarnings("unused") StateExecution execution) {
    try {
      Thread.sleep(10);
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
      Thread.sleep(500);
    } catch (@SuppressWarnings("unused") InterruptedException e) {
      // ignore
    }
    return NextAction.stopInState(END, "Goto end");
  }

  public void error(@SuppressWarnings("unused") StateExecution execution) {
    logger.error("should not happen");
  }
}
