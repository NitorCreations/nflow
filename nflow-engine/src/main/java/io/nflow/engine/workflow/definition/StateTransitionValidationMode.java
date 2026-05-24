package io.nflow.engine.workflow.definition;

/**
 * Defines which workflow state transitions are accepted when validating a transition.
 */
public enum StateTransitionValidationMode {

  /**
   * Allow only normal transitions configured in workflow definition state transitions.
   */
  allowNormal,

  /**
   * Allow only failure transition configured in the workflow definition.
   */
  allowFailure,

  /**
   * Allow both normal and failure transitions configured in workflow definition.
   */
  allowNormalAndFailure,

  /**
   * Disable transition validation and allow all transitions.
   */
  doNotValidate
}
