package io.nflow.engine.exception;

import static org.joda.time.Duration.standardMinutes;

import org.joda.time.Duration;

/**
 * Controls how an exception thrown while trying to save the workflow instance state should be handled.
 */
public class StateSaveExceptionHandling extends ExceptionHandling {
  /**
   * Retry delay.
   */
  public Duration retryDelay;

  StateSaveExceptionHandling(Builder builder) {
    super(builder);
    this.retryDelay = builder.retryDelay;
  }

  /**
   * Builder for exception handling settings.
   */
  public static class Builder extends ExceptionHandling.Builder<Builder> {
    Duration retryDelay = standardMinutes(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder getThis() {
      return this;
    }

    /**
     * Set the retry delay. Default is 1 minute.
     *
     * @param retryDelay
     *          The retry delay.
     * @return This.
     */
    public Builder setRetryDelay(Duration retryDelay) {
      this.retryDelay = retryDelay;
      return this;
    }

    /**
     * Create the state save exception handling object.
     *
     * @return State save exception handling.
     */
    @Override
    public StateSaveExceptionHandling build() {
      return new StateSaveExceptionHandling(this);
    }
  }
}
