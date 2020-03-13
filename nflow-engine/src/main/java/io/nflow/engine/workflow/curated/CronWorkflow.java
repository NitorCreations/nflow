package io.nflow.engine.workflow.curated;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.doWork;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.failed;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.handleFailure;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowSettings.Builder.oncePerDay;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.joda.time.Instant.now;
import static org.joda.time.Period.days;
import static org.slf4j.LoggerFactory.getLogger;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.scheduling.support.CronSequenceGenerator;

import io.nflow.engine.workflow.curated.CronWorkflow.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowSettings.Builder;
import io.nflow.engine.workflow.definition.WorkflowStateType;

/**
 * Workflow that wakes up periodically to execute a task.
 */
public abstract class CronWorkflow extends WorkflowDefinition<State> {
  private static final Logger logger = getLogger(CronWorkflow.class);

  /**
   * Name of the state variable that stores schedule in cron syntax.
   */
  public static final String VAR_SCHEDULE = "cron";

  /**
   * States of cron workflow.
   */
  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    schedule(start), doWork(normal), handleFailure(normal), failed(manual);

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
   * Extend cron workflow definition with customized workflow settings. It is recommended to enable the workflow state and action
   * history cleanup. Extending class must implement the 'doWork` state method. For example:
   *
   * <pre>
   * public NextAction doWork(StateExecution execution) {
   *   // do the work here
   *   return NextAction.moveToState(schedule, "Work done");
   * }
   * </pre>
   *
   * @param type
   *          The type of the workflow.
   * @param settings
   *          The workflow settings.
   */
  protected CronWorkflow(String type, WorkflowSettings settings) {
    super(type, schedule, handleFailure, settings);
    permit(schedule, doWork);
    permit(doWork, schedule);
    permit(handleFailure, schedule, failed);
  }

  /**
   * Extend cron workflow definition. Uses workflow settings that enable automatic workflow state and action history cleanup
   * (delete history older than 45 days, run cleanup once per day). Extending class must implement the 'doWork` state method. For
   * example:
   *
   * <pre>
   * public NextAction doWork(StateExecution execution) {
   *   // do the work here
   *   return NextAction.moveToState(schedule, "Work done");
   * }
   * </pre>
   *
   * @param type
   *          The type of the workflow.
   */
  protected CronWorkflow(String type) {
    this(type, new Builder().setHistoryDeletableAfter(days(45)).setDeleteHistoryCondition(oncePerDay()).build());
  }

  /**
   * Determines the next execution time for the doWork state by calling {@link getNextActivationTime}.
   *
   * @param execution
   *          The workflow execution context.
   * @param cron
   *          The cron state variable.
   * @return Action to go to doWork state at scheduled time.
   */
  public NextAction schedule(StateExecution execution, @StateVar(value = VAR_SCHEDULE, readOnly = true) String cron) {
    return moveToStateAfter(doWork, getNextActivationTime(execution, cron), "Scheduled");
  }

  /**
   * Calculates next activation time based on cron state variable. Override for custom scheduling.
   *
   * @param execution
   *          The workflow execution context.
   * @param cron
   *          The cron schedule.
   * @return The next activation time.
   */
  protected DateTime getNextActivationTime(StateExecution execution, String cron) {
    return new DateTime(new CronSequenceGenerator(cron).next(now().toDate()));
  }

  /**
   * Tries to handle failures by calling {@link #handleFailureImpl}.
   *
   * @param execution
   *          The workflow execution context.
   * @return Action to go to schedule or failed state.
   */
  public NextAction handleFailure(StateExecution execution) {
    if (handleFailureImpl(execution)) {
      return moveToState(schedule, "Failure handled, rescheduling");
    }
    return moveToState(failed, "Failure handling failed, stopping");
  }

  /**
   * Logs an error and continues. Override for custom error handling.
   *
   * @param execution
   *          The workflow execution context.
   * @return True if workflow should be rescheduled. False to go to failed state and wait for manual actions.
   */
  protected boolean handleFailureImpl(StateExecution execution) {
    logger.error("Cron workflow {} / {} work failed", getType(), execution.getWorkflowInstanceId());
    return true;
  }
}
