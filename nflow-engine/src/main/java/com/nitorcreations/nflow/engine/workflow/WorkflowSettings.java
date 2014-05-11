package com.nitorcreations.nflow.engine.workflow;

import static org.joda.time.DateTime.now;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class WorkflowSettings {

  private final Environment env;

  private final int errorTransitionDelay;
  private final int shortTransitionDelay;
  private final int immediateTransitionDelay;
  private final int maxRetries;

  @Inject
  public WorkflowSettings(Environment env) {
    this.env = env;
    errorTransitionDelay = getIntegerProperty("transition.delay.waiterror.ms", 7200000);
    shortTransitionDelay = getIntegerProperty("transition.delay.waitshort.ms", 30000);
    immediateTransitionDelay = getIntegerProperty("transition.delay.immediate.ms", 0);
    maxRetries = getIntegerProperty("max.state.retries", 3);
  }

  public DateTime getErrorTransitionActivation() {
    return now().plusMillis(getErrorTransitionDelay());
  }

  public int getErrorTransitionDelay() {
    return errorTransitionDelay;
  }

  public DateTime getShortTransitionActivation() {
    return now().plusMillis(getShortTransitionDelay());
  }

  public int getShortTransitionDelay() {
    return shortTransitionDelay;
  }

  public int getImmediateTransitionDelay() {
    return immediateTransitionDelay;
  }

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
