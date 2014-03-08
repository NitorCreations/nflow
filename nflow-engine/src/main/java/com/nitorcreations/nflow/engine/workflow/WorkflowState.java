package com.nitorcreations.nflow.engine.workflow;


public interface WorkflowState {

  String name();
  WorkflowStateType getType();

}
