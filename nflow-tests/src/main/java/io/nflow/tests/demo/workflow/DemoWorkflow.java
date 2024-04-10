package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.PROCESS;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

@Component
public class DemoWorkflow extends WorkflowDefinition {

  public static final String DEMO_WORKFLOW_TYPE = "demo";

  @SuppressWarnings("this-escape")
  public DemoWorkflow() {
    super(DEMO_WORKFLOW_TYPE, BEGIN, ERROR);
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
