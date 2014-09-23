package com.nitorcreations.nflow.engine.workflow.definition;

/**
 * Workflow state types.
 */
public enum WorkflowStateType {

  /**
   * Initial states of the workflow.
   */
  start(false),

  /**
   * State that requires manual action. Workflow execution is stopped.
   */
  manual(true),

  /**
   * State that can be normally executed.
   */
  normal(false),

  /**
   * Final state of the workflow. Workflow execution is stopped.
   */
  end(true);

  private final boolean isFinal;

  private WorkflowStateType(boolean isFinal) {
    this.isFinal = isFinal;
  }

  /**
   * Returns true is the state of this type is a final state, e.g. a state after
   * which the workflow processing is stopped. The workflow state can be moved
   * to another state only by manual action.
   *
   * @return True for states of type {@code manual} and {@code end}, false for
   *         states of type {@code start} and {@code normal}.
   */
  public boolean isFinal() {
    return isFinal;
  }
}
