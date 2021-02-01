package io.nflow.engine.workflow.definition;

import static org.slf4j.event.Level.ERROR;

import org.slf4j.event.Level;

public class ExceptionHandling {
  public final boolean isRetryable;
  public final Level logLevel;
  public final boolean logStackTrace;

  ExceptionHandling(boolean isRetryable, Level logLevel, boolean logStackTrace) {
    this.isRetryable = isRetryable;
    this.logLevel = logLevel;
    this.logStackTrace = logStackTrace;
  }

  public static class Builder {
    private boolean isRetryable = true;
    private Level logLevel = ERROR;
    private boolean logStackTrace = true;

    public Builder setRetryable(boolean isRetryable) {
      this.isRetryable = isRetryable;
      return this;
    }

    public Builder setLogLevel(Level logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public Builder setLogStackTrace(boolean logStackTrace) {
      this.logStackTrace = logStackTrace;
      return this;
    }

    public ExceptionHandling build() {
      return new ExceptionHandling(isRetryable, logLevel, logStackTrace);
    }
  }
}
