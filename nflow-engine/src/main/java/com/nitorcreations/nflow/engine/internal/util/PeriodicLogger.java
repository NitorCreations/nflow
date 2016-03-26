package com.nitorcreations.nflow.engine.internal.util;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import org.slf4j.Logger;

/**
 * PeriodicLogger logs once per given periodInSeconds period. Typically used in a loop where you don't want to log at every
 * iteration, but want to get a log row e.g. once per minute. Not thread safe.
 */
public class PeriodicLogger {
  private long previousLogging;
  private final int periodInSeconds;
  private final Logger logger;

  public PeriodicLogger(Logger logger, int periodInSeconds) {
    this.logger = logger;
    this.periodInSeconds = periodInSeconds;
  }

  public void info(String message, Object... parameters) {
    if (canLog()) {
      logger.info(message, parameters);
    }
  }

  public void warn(String message, Object... parameters) {
    if (canLog()) {
      logger.warn(message, parameters);
    }
  }

  private boolean canLog() {
    long currentPeriod = periodNumber();
    if (previousLogging != currentPeriod) {
      previousLogging = currentPeriod;
      return true;
    }
    return false;
  }

  private long periodNumber() {
    return currentTimeMillis() / 1000 / periodInSeconds;
  }
}
