package com.nitorcreations.nflow.performance.workflow;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static org.joda.time.DateTime.now;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

/**
 * Deterministic workflow that executes quickly.
 */
public class ConstantWorkflow extends WorkflowDefinition<ConstantWorkflow.ConstantState> {
  private static final Logger logger = LoggerFactory.getLogger(ConstantWorkflow.class);
  private final String key = "retries";

  public static enum ConstantState implements WorkflowState {
    start(WorkflowStateType.start, "Start"), quickState("This executes fast then goes to retryTwice"), retryTwiceState(
        "Retries twice and goes then goes to scheduleState"), scheduleState("Goes to slowState, in 3 sec"), slowState(
        "This executes bit slower. Goes to end"), end(WorkflowStateType.end, "End"), error(WorkflowStateType.end,
        "Error. Should not be used.");
    private final WorkflowStateType type;
    private final String description;

    private ConstantState(String description) {
      this(WorkflowStateType.normal, description);
    }

    private ConstantState(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public ConstantWorkflow() {
    super(ConstantWorkflow.class.getSimpleName(), ConstantState.start, ConstantState.error, new WorkflowSettings.Builder()
        .setMaxErrorTransitionDelay(5000).build());
    permit(ConstantState.start, ConstantState.quickState);
    permit(ConstantState.quickState, ConstantState.retryTwiceState);
    permit(ConstantState.retryTwiceState, ConstantState.scheduleState);
    permit(ConstantState.scheduleState, ConstantState.slowState);
    permit(ConstantState.slowState, ConstantState.end);
  }

  public NextAction start(StateExecution execution) {
    // nothing here
    execution.setVariable(key, 0);
    return moveToState(ConstantState.quickState, "Time for quickness");
  }

  public NextAction quickState(StateExecution execution) {
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      // ignore
    }
    return moveToState(ConstantState.retryTwiceState, "Go do some retries");
  }

  public NextAction retryTwiceState(StateExecution execution) {
    // Retries once and goes then goes to scheduleState
    Integer retryCount = execution.getVariable(key, Integer.class);
    retryCount++;
    execution.setVariable(key, retryCount);
    if (retryCount > 2) {
      logger.info("Retry count {}. Go to next state", retryCount);
      return moveToState(ConstantState.scheduleState, "Schedule some action");

    }
    throw new RuntimeException("Retry count " + retryCount + ". Retrying");
  }

  public NextAction scheduleState(StateExecution execution) {
    return moveToStateAfter(ConstantState.slowState, now().plusSeconds(3), "Schedule some action");
  }

  public NextAction slowState(StateExecution execution) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // ignore
    }
    return NextAction.stopInState(ConstantState.end, "Goto end");
  }

  public void error(StateExecution execution) {
    logger.error("should not happen");
  }
}
