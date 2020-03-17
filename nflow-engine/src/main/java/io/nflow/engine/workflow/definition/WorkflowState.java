package io.nflow.engine.workflow.definition;

/**
 * Provides access to the workflow state information.
 */
public interface WorkflowState {
  /**
   * Return the name of the workflow state.
   *
   * @return The name.
   */
  String name();

  /**
   * Return the workflow state type.
   *
   * @return The workflow state type.
   */
  WorkflowStateType getType();

  /**
   * Return the description of the workflow state. Default implementation returns {@link #name()}.
   *
   * @return The description.
   */
  default String getDescription() {
    return name();
  }

  /**
   * Return true if this state can be automatically retried after throwing an exception, or false if the workflow instance should
   * move directly to failure state. Default implementation returns true.
   *
   * @return True if the state can be retried.
   */
  default boolean isRetryAllowed() {
    return true;
  }
}
