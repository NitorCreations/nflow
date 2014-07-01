package com.nitorcreations.nflow.engine.workflow.definition;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
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
    errorTransitionDelay = getIntegerProperty("nflow.transition.delay.waiterror.ms", (int) HOURS.toMillis(2));
    shortTransitionDelay = getIntegerProperty("nflow.transition.delay.waitshort.ms", (int) SECONDS.toMillis(30));
    immediateTransitionDelay = getIntegerProperty("nflow.transition.delay.immediate.ms", 0);
    maxRetries = getIntegerProperty("nflow.max.state.retries", 3);
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
