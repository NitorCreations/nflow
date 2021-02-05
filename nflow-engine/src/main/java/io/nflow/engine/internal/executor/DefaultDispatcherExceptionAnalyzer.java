package io.nflow.engine.internal.executor;

import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import io.nflow.engine.exception.DispatcherExceptionHandling;
import io.nflow.engine.exception.DispatcherExceptionHandling.Builder;
import io.nflow.engine.internal.dao.PollingBatchException;
import io.nflow.engine.internal.dao.PollingRaceConditionException;

/**
 * Default dispatcher exception analyzer.
 */
@Component
public class DefaultDispatcherExceptionAnalyzer implements DispatcherExceptionAnalyzer {

  /**
   * {@inheritDoc}
   */
  @Override
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
