package io.nflow.engine.workflow.definition;

import static org.slf4j.event.Level.ERROR;

import org.slf4j.event.Level;

public class ExceptionSeverity {
  public static final ExceptionSeverity DEFAULT = new ExceptionSeverity(ERROR, true);
  public final Level logLevel;
  public final boolean logStackTrace;

  ExceptionSeverity(Level logLevel, boolean logStackTrace) {
    this.logLevel = logLevel;
    this.logStackTrace = logStackTrace;
  }

  public static class Builder {
    private Level logLevel = ERROR;
    private boolean logStackTrace = true;

    public void setLogLevel(Level logLevel) {
      this.logLevel = logLevel;
    }

    public void setLogStackTrace(boolean logStackTrace) {
      this.logStackTrace = logStackTrace;
    }

    public ExceptionSeverity build() {
      return new ExceptionSeverity(logLevel, logStackTrace);
    }
  }
}
