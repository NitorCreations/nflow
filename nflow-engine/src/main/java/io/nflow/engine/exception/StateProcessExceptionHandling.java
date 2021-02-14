package io.nflow.engine.exception;

/**
 * Controls how an exception thrown by a state method should be handled by the workflow state processor.
 */
public class StateProcessExceptionHandling extends ExceptionHandling {
  /**
   * True when the state method processing should be retried.
   */
  public final boolean isRetryable;

  StateProcessExceptionHandling(Builder builder) {
    super(builder);
    this.isRetryable = builder.isRetryable;
  }

  /**
   * Builder for exception handling settings.
   */
  public static class Builder extends ExceptionHandling.Builder<Builder> {
    boolean isRetryable = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder getThis() {
      return this;
    }

    /**
     * Set if state method processing is retryable or not.
     *
     * @param isRetryable
     *          True if state method processing should be retried.
     * @return This.
     */
    public Builder setRetryable(boolean isRetryable) {
      this.isRetryable = isRetryable;
      return this;
    }

    /**
     * Create the exception handling object.
     *
     * @return State process exception handling.
     */
    @Override
    public StateProcessExceptionHandling build() {
      return new StateProcessExceptionHandling(this);
    }
  }
}
