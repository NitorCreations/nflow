package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.PROCESS;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;

public class Demo2Workflow extends AbstractWorkflowDefinition {

  public static final String DEMO2_WORKFLOW_TYPE = "demo2";

  public Demo2Workflow() {
    super(DEMO2_WORKFLOW_TYPE, BEGIN, ERROR);
    setDescription("Simple demo workflow: start -> process -> end");
    permit(BEGIN, PROCESS);
    permit(PROCESS, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(PROCESS, "Go to process state");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Go to done state");
  }
}
