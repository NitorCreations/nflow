package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.PROCESS;

import org.joda.time.Period;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.instance.WorkflowInstance;

@Component
public class DeleteHistoryWorkflow extends AbstractWorkflowDefinition {

  public static final String TYPE = "deleteHistory";

  public DeleteHistoryWorkflow() {
    super(TYPE, BEGIN, ERROR,
        new WorkflowSettings.Builder().setHistoryDeletableAfter(Period.ZERO).setDeleteHistoryCondition(() -> true).build());
    setDescription("Delete history workflow");
    permit(BEGIN, PROCESS);
    permit(PROCESS, DONE);
  }

  public NextAction begin(StateExecution execution) {
    WorkflowInstance childWorkflow = new WorkflowInstance.Builder().setType(TYPE).build();
    execution.addChildWorkflows(childWorkflow);
    execution.setVariable("notDeletedVariable", "value");
    return moveToState(PROCESS, "Begin");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DONE, "Process");
  }

  public void done(@SuppressWarnings("unused") StateExecution execution) {
    // just delete history after processing
  }
}
