package io.nflow.engine.exception;

import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import io.nflow.engine.exception.DispatcherExceptionHandling.Builder;
import io.nflow.engine.internal.dao.PollingBatchException;
import io.nflow.engine.internal.dao.PollingRaceConditionException;

/**
 * Dispatcher exception analyzer analyzes exceptions thrown by the workflow dispatcher and determines how the exception is
 * handled.
 */
@Component
public class DispatcherExceptionAnalyzer {

  /**
   * Analyze the exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  public DispatcherExceptionHandling analyze(Exception e) {
    Builder builder = new DispatcherExceptionHandling.Builder();
    if (e instanceof PollingRaceConditionException) {
      builder.setLogLevel(Level.DEBUG).setLogStackTrace(false).setRandomizeSleep(true);
    } else if (e instanceof PollingBatchException) {
      builder.setLogLevel(Level.WARN).setLogStackTrace(false).setSleep(false);
    } else if (e instanceof InterruptedException) {
      builder.setLog(false).setSleep(false);
    }
    return builder.build();
  }
}
