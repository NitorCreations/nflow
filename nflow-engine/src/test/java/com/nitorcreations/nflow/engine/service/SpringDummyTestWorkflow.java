package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

@Component
public class SpringDummyTestWorkflow extends WorkflowDefinition<SpringDummyTestWorkflow.SpringDummyTestState> {

  public static enum SpringDummyTestState implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
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
    public String getName() {
      return name();
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  protected SpringDummyTestWorkflow() {
    super("springdummy", SpringDummyTestState.start, SpringDummyTestState.end);
  }

  public NextAction start(StateExecution execution) {
    return moveToState(SpringDummyTestState.end, "Go to end state");
  }

  public NextAction end(StateExecution execution) {
    return stopInState(SpringDummyTestState.end, "Stop in end state");
  }
}
