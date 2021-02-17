package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.POLL;
import static org.joda.time.DateTime.now;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;

@Component
public class ForeverWaitingWorkflow extends AbstractWorkflowDefinition {
  private static final Logger logger = LoggerFactory.getLogger(ForeverWaitingWorkflow.class);

  public static final String FOREVER_WAITING_WORKFLOW_TYPE = "foreverWaiting";

  public ForeverWaitingWorkflow() {
    super(FOREVER_WAITING_WORKFLOW_TYPE, BEGIN, ERROR);
    setDescription("Workflow that waits for a year in 'waiting' state");
    permit(BEGIN, POLL);
    permit(POLL, POLL);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    logger.info("in begin state");
    return moveToStateAfter(POLL, now().plusYears(1), "Move to waiting after an year");
  }

  public NextAction poll(@SuppressWarnings("unused") StateExecution execution) {
    logger.info("in waiting state");
    return moveToStateAfter(POLL, now().plusYears(1), "Move to waiting again after an year");
  }
}
