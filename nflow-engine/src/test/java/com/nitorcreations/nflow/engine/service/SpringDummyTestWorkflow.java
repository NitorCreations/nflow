package com.nitorcreations.nflow.engine.service;

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

  public void start(StateExecution execution) {
    execution.setNextState(SpringDummyTestState.end);
  }

  public void end(StateExecution execution) {
    execution.setNextState(SpringDummyTestState.end);
  }

}
