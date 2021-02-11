package io.nflow.engine.service;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowState;

@Component
@Profile("nflow-engine-test")
public class SpringDummyTestWorkflow extends AbstractWorkflowDefinition<WorkflowState> {

  protected SpringDummyTestWorkflow() {
    super("springdummy", BEGIN, DONE);
    permit(BEGIN, DONE);
  }

  public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DONE, "Go to end state");
  }
}
