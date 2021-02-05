package io.nflow.engine.exception;

import static org.slf4j.event.Level.ERROR;

import org.slf4j.event.Level;

/**
 * Controls how an exception should be handled.
 */
public class ExceptionHandling {
  /**
   * The log entry level for logging the exception.
   */
  public final Level logLevel;
  /**
   * True when the exception stack trace should be logged.
   */
  public final boolean logStackTrace;

  ExceptionHandling(Builder<?> builder) {
    this.logLevel = builder.logLevel;
    this.logStackTrace = builder.logStackTrace;
  }

  /**
   * Builder for exception handling settings.
   */
  public abstract static class Builder<T extends Builder<T>> {
    Level logLevel = ERROR;
    boolean logStackTrace = true;

    /**
     * Return this.
     *
     * @return This.
     */
    public abstract T getThis();

    /**
     * Set the log entry level. Default is ERROR.
     *
     * @param logLevel
     *          The log entry level.
     * @return This.
     */
    public T setLogLevel(Level logLevel) {
      this.logLevel = logLevel;
      return getThis();
    }

    /**
     * Set if exception stack trace should be logged or not. Default is true.
     *
     * @param logStackTrace
     *          True to log the exception stack trace, false to log the exception message only.
     * @return This.
     */
    public T setLogStackTrace(boolean logStackTrace) {
      this.logStackTrace = logStackTrace;
      return getThis();
    }

    /**
     * Create the exception handling object.
     *
     * @return Exception handling.
     */
    public ExceptionHandling build() {
      return new ExceptionHandling(getThis());
    }
  }
}
