package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.CronWorkflow.State.done;
import static io.nflow.engine.workflow.definition.CronWorkflow.State.error;
import static io.nflow.engine.workflow.definition.CronWorkflow.State.executeTask;
import static io.nflow.engine.workflow.definition.CronWorkflow.State.handleFailure;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.slf4j.LoggerFactory.getLogger;

import org.joda.time.DateTime;
import org.slf4j.Logger;

/**
 * A workflow that executes a task at given times. Similar to cron job or scheduled method but guarantees single execution in the
 * cluster.
 */
public abstract class CronWorkflow extends WorkflowDefinition<CronWorkflow.State> {

  private static final Logger logger = getLogger(CronWorkflow.class);

  /**
   * The workflow states.
   */
  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    executeTask(start), handleFailure(normal), done(end), error(manual);

    private WorkflowStateType type;

    State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  /**
   * Call this from your cron workflow implementation constructor.
   *
   * @param type
   *          The type of your cron workflow
   * @param settings
   *          The workflow settings
   */
  protected CronWorkflow(String type, WorkflowSettings settings) {
    super(type, executeTask, handleFailure, settings);
    permit(executeTask, executeTask);
    permit(executeTask, done);
    permit(handleFailure, executeTask, error);
    permit(handleFailure, done);
  }

  /**
   * Executes the task and reschedules it or goes to the `done` state.
   *
   * @param execution
   *          State execution
   * @return The next action
   */
  public NextAction executeTask(StateExecution execution) {
    if (executeTaskImpl(execution)) {
      return moveToStateAfter(executeTask, getNextExecutionTime(), "Task executed successfully");
    }
    return moveToState(done, "Task executed successfully");
  }

  /**
   * Handles the failure and reschedules the task or goes to the `done` state.
   *
   * @param execution
   *          State execution
   * @return The next action
   */
  public NextAction handleFailure(StateExecution execution) {
    if (handleFailureImpl(execution)) {
      return moveToStateAfter(executeTask, getNextExecutionTime(), "Failure handled successfully");
    }
    return moveToState(done, "Failure handled successfully");
  }

  /**
   * Override this method to execute the task.
   *
   * @param execution
   *          State execution
   * @return True if the task should be rescheduled, false otherwise.
   */
  protected abstract boolean executeTaskImpl(StateExecution execution);

  /**
   * Override this method to implement scheduling.
   *
   * @return The next time when the task should be executed.
   */
  protected abstract DateTime getNextExecutionTime();

  /**
   * Override this method to handle persistent failures. Default implementation logs an error and returns false to avoid
   * rescheduling the task execution.
   *
   * @param execution
   *          State execution
   * @return True if the task should be rescheduled, false otherwise.
   */
  protected boolean handleFailureImpl(StateExecution execution) {
    logger.error("Persistent failure in task execution of cron workflow {}. Task is not rescheduled.", getType());
    return false;
  }

}
