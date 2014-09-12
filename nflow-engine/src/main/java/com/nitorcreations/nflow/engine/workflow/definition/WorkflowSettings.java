package com.nitorcreations.nflow.engine.workflow.definition;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.DateTime.now;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configuration for the workflow execution.
 */
@Component
public class WorkflowSettings {

  private final Environment env;

  private final int errorTransitionDelay;
  private final int shortTransitionDelay;
  private final int immediateTransitionDelay;
  private final int maxRetries;

  /**
   * Create workflow settings.
   *
   * @param env
   *          Spring environment.
   */
  @Inject
  public WorkflowSettings(Environment env) {
    this.env = env;
    errorTransitionDelay = getIntegerProperty("nflow.transition.delay.waiterror.ms", (int) HOURS.toMillis(2));
    shortTransitionDelay = getIntegerProperty("nflow.transition.delay.waitshort.ms", (int) SECONDS.toMillis(30));
    immediateTransitionDelay = getIntegerProperty("nflow.transition.delay.immediate.ms", 0);
    maxRetries = getIntegerProperty("nflow.max.state.retries", 3);
  }

  /**
   * Return the next activation time for the workflow after an error.
   *
   * @return Next activation time.
   */
  public DateTime getErrorTransitionActivation() {
    return now().plusMillis(getErrorTransitionDelay());
  }

  /**
   * Return the delay before next activation after an error.
   *
   * @return The delay in milliseconds.
   */
  public int getErrorTransitionDelay() {
    return errorTransitionDelay;
  }

  /**
   * Return the next activation time for the workflow after detecting a busy
   * loop.
   *
   * @return Next activation time.
   */
  public DateTime getShortTransitionActivation() {
    return now().plusMillis(getShortTransitionDelay());
  }

  /**
   * Return the delay before next activation after detecting a busy loop.
   *
   * @return The delay in milliseconds.
   */
  public int getShortTransitionDelay() {
    return shortTransitionDelay;
  }

  /**
   * Return the delay after moving to another state.
   *
   * @return The delay in milliseconds.
   */
  public int getImmediateTransitionDelay() {
    return immediateTransitionDelay;
  }

  /**
   * Return the maximum number of retry attempts of the workflow state before
   * moving to error state.
   *
   * @return The maximum retry count.
   */
  public int getMaxRetries() {
    return maxRetries;
  }

  private int getIntegerProperty(String key, int defaultValue) {
    if (env != null) {
      return env.getRequiredProperty(key, Integer.class);
    }
    return defaultValue;
  }

}
