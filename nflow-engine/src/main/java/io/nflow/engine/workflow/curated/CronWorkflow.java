package io.nflow.engine.workflow.curated;

import io.nflow.engine.workflow.curated.CronWorkflow.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowSettings.Builder;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.scheduling.support.CronSequenceGenerator;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.doWork;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.failed;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.handleFailure;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.joda.time.Instant.now;
import static org.joda.time.Period.weeks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cron workflow executor that periodically wakes up.
 */
public abstract class CronWorkflow extends WorkflowDefinition<State> {
  private static final Logger logger = getLogger(CronWorkflow.class);

  /**
   * Name of the state variable that stores schedule in cron syntax.
   */
  public static final String VAR_SCHEDULE = "cron";

  public enum State implements WorkflowState {
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
   * Extend cron workflow definition with customized workflow settings.
   * Extending class must implement the 'doWork` step method.
   *
   * @param type The type of the workflow.
   * @param settings The workflow settings.
   */
  protected CronWorkflow(String type, WorkflowSettings settings) {
    super(type, schedule, handleFailure, settings);
    permit(schedule, doWork);
    permit(doWork, schedule);
    permit(handleFailure, schedule, failed);
  }

  /**
   * Extend cron workflow definition.
   * Extending class must implement the 'doWork` step method.
   *
   * @param type The type of the workflow.
   */
  protected CronWorkflow(String type) {
    this(type, new Builder().setHistoryDeletableAfter(weeks(1)).build());
  }

  /**
   * Calculates the next activation time.
   *
   * @param execution The workflow execution context.
   * @param cron The cron schedule.
   * @return The next activation time.
   */
  protected DateTime getNextActivationTime(@SuppressWarnings("unused") StateExecution execution, String cron) {
    return new DateTime(new CronSequenceGenerator(cron).next(now().toDate()));
  }

  /**
   * Handles errors in working step.
   *
   * @param execution The workflow execution context.
   * @return True if the failure was handled successfully. If false is returned the cron workflow stops waiting for manual actions.
   */
  protected boolean handleFailureImpl(StateExecution execution) {
    logger.error("Cron workflow {} / {} work failed", getType(), execution.getWorkflowInstanceId());
    return true;
  }

  public NextAction schedule(StateExecution execution, @StateVar(value = VAR_SCHEDULE, readOnly = true) String cron) {
    return moveToStateAfter(doWork, getNextActivationTime(execution, cron), "Scheduled");
  }

  public NextAction handleFailure(StateExecution execution) {
    if (handleFailureImpl(execution)) {
      return moveToState(schedule, "Failure handled successfully");
    }
    return moveToState(failed, "Failure handling failed");
  }
}
