package com.nitorcreations.nflow.engine.workflow.definition;

/**
 * Workflow state types.
 */
public enum WorkflowStateType {

  /**
   * Initial states of the workflow.
   */
  start,

  /**
   * State that requires manual action. Workflow execution is stopped.
   */
  manual,

  /**
   * State that can be normally executed.
   */
  normal,

  /**
   * Final state of the workflow. Workflow execution is stopped.
   */
  end;
}
