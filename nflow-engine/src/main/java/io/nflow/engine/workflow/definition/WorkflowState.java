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
   * move directly to failure state. Default implementation returns true if the throwable class is not annotated with
   * {@code @NonRetryable}.
   *
   * @param thrown
   *          The thrown exception.
   * @return True if the state can be retried.
   */
  default boolean isRetryAllowed(Throwable thrown) {
    return !thrown.getClass().isAnnotationPresent(NonRetryable.class);
  }

  /**
   * Return the severity of the exception thrown by the state execution. Using default means ERROR level logging with stack trace.
   *
   * @param thrown
   *          The thrown exception.
   * @return Exception severity.
   */
  default ExceptionSeverity getExceptionSeverity(Throwable thrown) {
    return ExceptionSeverity.DEFAULT;
  }
}
