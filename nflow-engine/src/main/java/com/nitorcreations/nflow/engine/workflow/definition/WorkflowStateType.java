package com.nitorcreations.nflow.engine.workflow.definition;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Workflow state types.
 */
public enum WorkflowStateType {

  /**
   * Initial states of the workflow.
   */
  start(false, WorkflowInstanceStatus.inProgress),

  /**
   * State that requires manual action. Workflow execution is stopped.
   */
  manual(true, WorkflowInstanceStatus.manual),

  /**
   * State that can be normally executed.
   */
  normal(false, WorkflowInstanceStatus.inProgress),

  /**
   * Final state of the workflow. Workflow execution is stopped.
   */
  end(true, WorkflowInstanceStatus.finished);

  private final boolean isFinal;
  private WorkflowInstanceStatus status;

  private WorkflowStateType(boolean isFinal, WorkflowInstanceStatus status) {
    this.isFinal = isFinal;
    this.status = status;
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

  /**
   * Returns the status for this type. This is used when a workflow instance state is updated, the new status of the instance will
   * be determined based on the new state type.
   *
   * @return Default status for this state type.
   */
  public WorkflowInstanceStatus getStatus() {
    return status;
  }
}
