package com.nitorcreations.nflow.engine.internal.util;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import org.slf4j.Logger;

/**
 * PeriodicLogger logs once per given periodInSeconds period. Typically used in a loop where you don't want to log at every
 * iteration, but want to get a log row e.g. once per minute. Not thread safe.
 */
public class PeriodicLogger {
  private Long previousLogging;
  private final int periodInSeconds;
  private final Logger logger;

  public PeriodicLogger(Logger logger, int periodInSeconds) {
    this.logger = logger;
    this.periodInSeconds = periodInSeconds;
  }

  public void log(String message, Object... parameters) {
    long now = periodNumber();
    if (previousLogging == null || previousLogging != now) {
      logger.info(message, parameters);
    }
    previousLogging = now;
  }

  private long periodNumber() {
    return currentTimeMillis() / 1000 / periodInSeconds;
  }
}
