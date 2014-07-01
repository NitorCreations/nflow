package com.nitorcreations.nflow.engine.workflow.definition;


public interface WorkflowState {
    String name();
    WorkflowStateType getType();
    String getName();
    String getDescription();
}
