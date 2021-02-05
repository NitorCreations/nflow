package io.nflow.engine.exception;

import static org.joda.time.Duration.standardSeconds;

import javax.inject.Inject;

import org.joda.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * {@inheritDoc}
 */
@Component
public class DefaultStateSaveExceptionAnalyzer implements StateSaveExceptionAnalyzer {

  private final StateSaveExceptionHandling handling;

  /**
   * Create state save exception analyzer.
   *
   * @param env
   *          The Spring environment.
   */
  @Inject
  public DefaultStateSaveExceptionAnalyzer(Environment env) {
    Duration retryDelay = standardSeconds(env.getRequiredProperty("nflow.executor.stateSaveRetryDelay.seconds", Long.class));
    handling = new StateSaveExceptionHandling.Builder().setRetryDelay(retryDelay).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateSaveExceptionHandling analyze(Exception e, int saveRetryCount) {
    return handling;
  }
}
