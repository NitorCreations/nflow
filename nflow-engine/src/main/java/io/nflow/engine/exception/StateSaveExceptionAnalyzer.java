package io.nflow.engine.exception;

import org.springframework.stereotype.Component;

/**
 * State save exception analyzer analyzes exceptions thrown while trying to save workflow state and determines how the exception
 * is handled.
 */
@Component
public interface StateSaveExceptionAnalyzer {

  /**
   * Analyze the exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @param saveRetryCount
   *          How many times the saving has been attempted before this attempt.
   * @return How the exception should be handled.
   */
  StateSaveExceptionHandling analyze(Exception e, int saveRetryCount);
}
