package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

@Component
public class BusinessKeyWorkflow extends WorkflowDefinition {

  public static final String BUSINESS_KEY_WORKFLOW_TYPE = "businessKeyWorkflow";

  @SuppressWarnings("this-escape")
  public BusinessKeyWorkflow() {
    super(BUSINESS_KEY_WORKFLOW_TYPE, TestState.BEGIN, TestState.ERROR);
    setDescription("Workflow that updates business key");
    permit(TestState.BEGIN, TestState.PROCESS);
    permit(TestState.PROCESS, TestState.DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(TestState.PROCESS, "Go to process state");
  }

  public NextAction process(StateExecution execution) {
    execution.setBusinessKey("newBusinessKey");
    return stopInState(TestState.DONE, "Go to done state");
  }
}
