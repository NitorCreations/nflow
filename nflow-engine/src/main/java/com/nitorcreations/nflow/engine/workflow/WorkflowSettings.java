package com.nitorcreations.nflow.engine.workflow;

import static org.joda.time.DateTime.now;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class WorkflowSettings {

  private final Environment env;

  @Inject
  public WorkflowSettings(Environment env) {
    this.env = env;
  }

  public DateTime getErrorTransitionActivation() {
    return now().plusMillis(getErrorTransitionDelay());
  }

  public int getErrorTransitionDelay() {
    return getIntegerProperty("transition.delay.waiterror.ms", 7200000);
  }

  public DateTime getShortTransitionActivation() {
    return now().plusMillis(getShortTransitionDelay());
  }

  public int getShortTransitionDelay() {
    return getIntegerProperty("transition.delay.waitshort.ms", 30000);
  }

  public int getImmediateTransitionDelay() {
    return getIntegerProperty("transition.delay.immediate.ms", 0);
  }

  public int getMaxRetries() {
    return getIntegerProperty("max.state.retries", 3);
  }

  private int getIntegerProperty(String key, int defaultValue) {
    if (env != null) {
      return env.getRequiredProperty(key, Integer.class);
    }
    return defaultValue;
  }

}
