package io.nflow.engine.workflow.definition;

import static org.slf4j.event.Level.ERROR;

import org.slf4j.event.Level;

/**
 * Controls how an exception thrown by a state method should be handled by the workflow state processor.
 */
public class ExceptionHandling {
  /**
   * True when the state method processing should be retried.
   */
  public final boolean isRetryable;
  /**
   * The log entry level for logging the exception.
   */
  public final Level logLevel;
  /**
   * True when the exception stack trace of the exception should be logged. False to log only exception message.
   */
  public final boolean logStackTrace;

  ExceptionHandling(boolean isRetryable, Level logLevel, boolean logStackTrace) {
    this.isRetryable = isRetryable;
    this.logLevel = logLevel;
    this.logStackTrace = logStackTrace;
  }

  /**
   * Builder for exception handling settings.
   */
  public static class Builder {
    private boolean isRetryable = true;
    private Level logLevel = ERROR;
    private boolean logStackTrace = true;

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
     * Set the log entry level.
     *
     * @param logLevel
     *          The log entry level.
     * @return This.
     */
    public Builder setLogLevel(Level logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    /**
     * Set if exception stack trace should be logged or not.
     *
     * @param logStackTrace
     *          True to log the exception stack trace, false to log the exception message only.
     * @return This.
     */
    public Builder setLogStackTrace(boolean logStackTrace) {
      this.logStackTrace = logStackTrace;
      return this;
    }

    /**
     * Create the exception handling object.
     *
     * @return Exception handling.
     */
    public ExceptionHandling build() {
      return new ExceptionHandling(isRetryable, logLevel, logStackTrace);
    }
  }
}
