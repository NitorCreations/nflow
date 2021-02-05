package io.nflow.engine.internal.executor;

public interface DispatcherExceptionAnalyzer {
  DispatcherExceptionHandling analyze(Exception e);
}
