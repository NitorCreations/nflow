package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static org.joda.time.DateTime.now;
import static org.joda.time.Period.hours;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.CronWorkflow;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings.Builder;
import io.nflow.engine.workflow.instance.WorkflowInstance;

@Component
public class TestCronWorkflow extends CronWorkflow {
  public static final String TYPE = "testCron";

  protected TestCronWorkflow() {
    super(TYPE, new Builder().setHistoryDeletableAfter(hours(1)).setDeleteHistoryCondition(() -> true).build());
  }

  public NextAction doWork(StateExecution execution) {
    WorkflowInstance childWorkflow = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE).build();
    execution.addChildWorkflows(childWorkflow);
    return moveToStateAfter(WAIT_FOR_WORK_TO_FINISH, now().plusSeconds(1), "Work delegated to child workflow");
  }

  @Override
  protected DateTime getNextActivationTime(StateExecution execution, String cron) {
    return super.getNextActivationTime(execution, cron);
  }

  @Override
  protected boolean handleFailureImpl(StateExecution execution) {
    return super.handleFailureImpl(execution);
  }

  @Override
  protected DateTime waitForWorkToFinishImpl(StateExecution execution) {
    return super.waitForWorkToFinishImpl(execution);
  }
}
