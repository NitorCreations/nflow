package io.nflow.engine.internal.executor;

import static org.slf4j.event.Level.ERROR;

import org.slf4j.event.Level;

/**
 * Controls how an exception should be handled by the dispatcher.
 */
public class DispatcherExceptionHandling {
  /**
   * True when dispatcher should log the exception.
   */
  public final boolean log;
  /**
   * The log entry level for logging the exception.
   */
  public final Level logLevel;
  /**
   * True when the exception stack trace of the exception should be logged.
   */
  public final boolean logStackTrace;
  /**
   * True when dispatcher should sleep a while after exception.
   */
  public final boolean sleep;
  /**
   * True when the sleep time should be randomized.
   */
  public final boolean randomizeSleep;

  DispatcherExceptionHandling(boolean log, Level logLevel, boolean logStackTrace, boolean sleep, boolean randomizeSleep) {
    this.log = log;
    this.logLevel = logLevel;
    this.logStackTrace = logStackTrace;
    this.sleep = sleep;
    this.randomizeSleep = randomizeSleep;
  }

  /**
   * Builder for exception handling settings.
   */
  public static class Builder {
    private boolean log = true;
    private Level logLevel = ERROR;
    private boolean logStackTrace = true;
    private boolean sleep = true;
    private boolean randomizeSleep = false;

    /**
     * Set if dispatcher should log the exception or not.
     *
     * @param log
     *          True if dispatcher should log the exception.
     * @return This.
     */
    public Builder setLog(boolean log) {
      this.log = log;
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
     *          True to log the exception stack trace.
     * @return This.
     */
    public Builder setLogStackTrace(boolean logStackTrace) {
      this.logStackTrace = logStackTrace;
      return this;
    }

    /**
     * Set if dispatcher should sleep a while after exception or not.
     *
     * @param sleep
     *          True if dispatcher should sleep a while after exception.
     * @return This.
     */
    public Builder setSleep(boolean sleep) {
      this.sleep = sleep;
      return this;
    }

    /**
     * Set if sleep time should be randomized or not.
     *
     * @param randomizeSleep
     *          True if sleep time should be randomized.
     * @return This.
     */
    public Builder setRandomizeSleep(boolean randomizeSleep) {
      this.randomizeSleep = randomizeSleep;
      return this;
    }

    /**
     * Create the dispatcher exception handling object.
     *
     * @return Dispatcher exception handling.
     */
    public DispatcherExceptionHandling build() {
      return new DispatcherExceptionHandling(log, logLevel, logStackTrace, sleep, randomizeSleep);
    }
  }
}
