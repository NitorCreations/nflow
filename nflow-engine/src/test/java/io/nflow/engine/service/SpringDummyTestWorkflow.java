package io.nflow.engine.service;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@Component
@Profile("nflow-engine-test")
public class SpringDummyTestWorkflow extends WorkflowDefinition<SpringDummyTestWorkflow.SpringDummyTestState> {

  public static enum SpringDummyTestState implements io.nflow.engine.workflow.definition.WorkflowState {
    start(WorkflowStateType.start), end(WorkflowStateType.end);

    private WorkflowStateType type;

    private SpringDummyTestState(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  protected SpringDummyTestWorkflow() {
    super("springdummy", SpringDummyTestState.start, SpringDummyTestState.end);
    permit(SpringDummyTestState.start, SpringDummyTestState.end);
  }

  public NextAction start(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(SpringDummyTestState.end, "Go to end state");
  }
}
