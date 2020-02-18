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
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.util.Date;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.doWork;
import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.joda.time.Period.weeks;

/**
 * Cron workflow executor that periodically wakes up.
 */
public abstract class CronWorkflow extends WorkflowDefinition<State> {
  public static final String VAR_SCHEDULE = "schedule";

  public enum State implements WorkflowState {
    schedule(start), doWork(normal);

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
    super(type, schedule, schedule, settings);
    setDescription("Wakes up to cron schedule to do work.");
    permit(schedule, doWork);
    permit(doWork, schedule);
  }

  protected CronWorkflow(String type) {
    this(type, new Builder()
            .setMaxRetries(10)
            .setMaxSubsequentStateExecutions(2)
            .setHistoryDeletableAfter(weeks(1))
            .build());
  }

  public NextAction schedule(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_SCHEDULE, readOnly = true) String schedule) {
    return moveToStateAfter(doWork, new DateTime(new CronSequenceGenerator(schedule).next(new Date())), "schedule");
  }

  public NextAction doWork(StateExecution execution) {
    workImpl(execution);
    return moveToState(schedule, "work done");
  }

  protected abstract void workImpl(StateExecution execution);
}
