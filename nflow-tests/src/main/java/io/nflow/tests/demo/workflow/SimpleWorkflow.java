package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;

public class SimpleWorkflow extends AbstractWorkflowDefinition {

  public static final String SIMPLE_WORKFLOW_TYPE = "simple";

  public SimpleWorkflow() {
    super(SIMPLE_WORKFLOW_TYPE, BEGIN, ERROR);
    setDescription("Simple demo workflow: start -> done");
    permit(BEGIN, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Finished");
  }
}
