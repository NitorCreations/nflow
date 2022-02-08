package io.nflow.engine.exception;

import static org.joda.time.Duration.standardSeconds;
import static org.slf4j.LoggerFactory.getLogger;

import org.joda.time.Duration;
import org.slf4j.Logger;

import io.nflow.engine.config.NFlowConfiguration;

/**
 * State save exception analyzer analyzes exceptions thrown while trying to save workflow state and determines how the exception
 * is handled.
 */
public class StateSaveExceptionAnalyzer {

  private static final Logger logger = getLogger(StateSaveExceptionAnalyzer.class);

  private final StateSaveExceptionHandling handling;

  /**
   * Create state save exception analyzer.
   *
   * @param env
   *          The Spring environment.
   */
  public StateSaveExceptionAnalyzer(NFlowConfiguration config) {
    Duration retryDelay = standardSeconds(config.getRequiredProperty("nflow.executor.stateSaveRetryDelay.seconds", Long.class));
    handling = new StateSaveExceptionHandling.Builder().setRetryDelay(retryDelay).build();
  }

  /**
   * Analyze the exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @param saveRetryCount
   *          How many times the saving has been attempted before this attempt.
   * @return How the exception should be handled.
   */
  public final StateSaveExceptionHandling analyzeSafely(Exception e, int saveRetryCount) {
    try {
      return analyze(e, saveRetryCount);
    } catch (Exception analyzerException) {
      logger.error("Failed to analyze exception, using default handling.", analyzerException);
    }
    return handling;
  }

  /**
   * Override this to provide custom handling.
   *
   * @param e
   *          The exception to be analyzed.
   * @param saveRetryCount
   *          How many times the saving has been attempted before this attempt.
   * @return How the exception should be handled.
   */
  protected StateSaveExceptionHandling analyze(Exception e, int saveRetryCount) {
    return handling;
  }
}
