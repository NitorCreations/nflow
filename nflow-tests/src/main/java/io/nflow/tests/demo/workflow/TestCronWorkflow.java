package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static org.joda.time.Period.hours;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.CronWorkflow;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings.Builder;

@Component
public class TestCronWorkflow extends CronWorkflow {
  public static final String TYPE = "testCron";

  protected TestCronWorkflow() {
    super(TYPE, new Builder().setHistoryDeletableAfter(hours(1)).setDeleteHistoryCondition(() -> true).build());
  }

  public NextAction doWork(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(SCHEDULE, "ok");
  }

  @Override
  protected DateTime getNextActivationTime(StateExecution execution, String cron) {
    return super.getNextActivationTime(execution, cron);
  }

  @Override
  protected boolean handleFailureImpl(StateExecution execution) {
    return super.handleFailureImpl(execution);
  }
}
