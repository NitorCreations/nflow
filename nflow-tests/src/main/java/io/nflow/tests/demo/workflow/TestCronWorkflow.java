package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.CronWorkflow;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;

@Component
public class TestCronWorkflow extends CronWorkflow {
  public static final String TYPE = "testCron";

  protected TestCronWorkflow() {
    super(TYPE);
  }

  public NextAction doWork(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(schedule, "ok");
  }
}
