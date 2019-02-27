package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.WorkflowInstance;

@Component
public class DeleteHistoryWorkflow extends WorkflowDefinition<DeleteHistoryWorkflow.State> {

  public static final String TYPE = "deleteHistory";

  public static enum State implements WorkflowState {
    begin(start), process(normal), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  public DeleteHistoryWorkflow() {
    super(TYPE, State.begin, State.error,
        new WorkflowSettings.Builder().setHistoryDeletableAfterHours(0).setDeleteHistoryCondition(() -> true).build());
    setDescription("Delete history workflow");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(StateExecution execution) {
    WorkflowInstance childWorkflow = new WorkflowInstance.Builder().setType(TYPE).build();
    execution.addChildWorkflows(childWorkflow);
    execution.setVariable("notDeletedVariable", "value");
    return moveToState(State.process, "Begin");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(State.done, "Process");
  }

  public void done(@SuppressWarnings("unused") StateExecution execution) {
    // just delete history after processing
  }

}
