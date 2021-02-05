package io.nflow.engine.internal.executor;

/**
 * Dispatcher exception analyzer analyzes exceptions throws by the workflow dispatcher and determines how the exception is
 * handled.
 */
public interface DispatcherExceptionAnalyzer {

  /**
   * Analyze the exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  DispatcherExceptionHandling analyze(Exception e);
}
