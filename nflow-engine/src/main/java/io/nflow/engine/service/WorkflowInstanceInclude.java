package io.nflow.engine.service;

/**
 * The properties of a workflow instance that can be loaded if needed when using WorkflowInstanceService.getWorkflowInstance.
 */
public enum WorkflowInstanceInclude {

  /**
   * The most recent values of all state variables (WorkflowInstance.stateVariables).
   */
  CURRENT_STATE_VARIABLES,

  /**
   * The child workflow identifiers (WorkflowInstance.childWorkflows).
   */
  CHILD_WORKFLOW_IDS,

  /**
   * The actions related to this workflow instance (WorkflowInstance.actions).
   */
  ACTIONS,

  /**
   * The state variables for each action. Ignored if ACTIONS are not loaded (WorkflowInstanceAction.updatedStateVariables).
   */
  ACTION_STATE_VARIABLES

}
