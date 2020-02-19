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
import static org.joda.time.DateTime.now;
import static org.joda.time.Period.weeks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cron workflow executor that periodically wakes up.
 */
public abstract class CronWorkflow extends WorkflowDefinition<State> {
  private static final Logger logger = getLogger(CronWorkflow.class);

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

  protected CronWorkflow(String type, WorkflowSettings settings) {
    super(type, schedule, handleFailure, settings);
    permit(schedule, doWork);
    permit(doWork, schedule);
    permit(handleFailure, schedule, failed);
  }

  protected CronWorkflow(String type) {
    this(type, new Builder().setHistoryDeletableAfter(weeks(1)).build());
  }

  public NextAction schedule(StateExecution execution, @StateVar(value = VAR_SCHEDULE, readOnly = true) String cron) {
    return moveToStateAfter(doWork, getNextActivationTime(cron, execution.getRequestedActivationTime()), "Scheduled");
  }

  protected DateTime getNextActivationTime(String cron, DateTime lastWorkEndTime) {
    DateTime next = new DateTime(new CronSequenceGenerator(cron).next(lastWorkEndTime.toDate()));
    return next.isBeforeNow() ? now() : next;
  }

  public NextAction handleFailure(StateExecution execution) {
    if (handleFailureImpl(execution)) {
      return moveToState(schedule, "Failure handled successfully");
    }
    return moveToState(failed, "Failure handling failed");
  }

  protected boolean handleFailureImpl(StateExecution execution) {
    logger.error("Cron workflow {} / {} work failed", getType(), execution.getWorkflowInstanceId());
    return true;
  }
}
