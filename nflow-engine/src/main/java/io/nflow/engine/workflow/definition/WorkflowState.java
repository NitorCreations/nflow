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
}
