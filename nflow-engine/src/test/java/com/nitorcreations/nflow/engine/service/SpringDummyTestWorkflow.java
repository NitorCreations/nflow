package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.nflow.engine.workflow.definition.NextState.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextState.stopInState;

import com.nitorcreations.nflow.engine.workflow.definition.NextState;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class SpringDummyTestWorkflow extends WorkflowDefinition<SpringDummyTestWorkflow.SpringDummyTestState> {

  public static enum SpringDummyTestState implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
    start, end;

    @Override
    public WorkflowStateType getType() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

  }

  protected SpringDummyTestWorkflow() {
    super("springdummy", SpringDummyTestState.start, SpringDummyTestState.end);
  }

  public NextState start(StateExecution execution) {
    return moveToState(SpringDummyTestState.end, "Go to end state");
  }

  public NextState end(StateExecution execution) {
    return stopInState(SpringDummyTestState.end, "Stop in end state");
  }
}
