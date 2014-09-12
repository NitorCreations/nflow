package com.nitorcreations.nflow.engine.workflow.definition;

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;

public class WorkflowSettings {
  public final int minErrorTransitionDelay;
  public final int maxErrorTransitionDelay;
  public final int shortTransitionDelay;
  public final int immediateTransitionDelay;
  public final int maxRetries;

  private WorkflowSettings(Builder builder) {
    this.minErrorTransitionDelay = builder.minErrorTransitionDelay;
    this.maxErrorTransitionDelay = builder.maxErrorTransitionDelay;
    this.shortTransitionDelay = builder.shortTransitionDelay;
    this.immediateTransitionDelay = builder.immediateTransitionDelay;
    this.maxRetries = builder.maxRetries;
  }

  public static class Builder {
    public int maxErrorTransitionDelay;
    public int minErrorTransitionDelay;
    public int shortTransitionDelay;
    public int immediateTransitionDelay;
    public int maxRetries;

    public Builder() {
      this(null);
    }

    public Builder(Environment env) {
      minErrorTransitionDelay = getIntegerProperty(env, "nflow.transition.delay.error.min.ms", (int) MINUTES.toMillis(1));
      maxErrorTransitionDelay = getIntegerProperty(env, "nflow.transition.delay.error.max.ms", (int) DAYS.toMillis(1));
      shortTransitionDelay = getIntegerProperty(env, "nflow.transition.delay.waitshort.ms", (int) SECONDS.toMillis(30));
      immediateTransitionDelay = getIntegerProperty(env, "nflow.transition.delay.immediate.ms", 0);
      maxRetries = getIntegerProperty(env, "nflow.max.state.retries", 17);
    }

    public Builder setMaxErrorTransitionDelay(int maxErrorTransitionDelay) {
      this.maxErrorTransitionDelay = maxErrorTransitionDelay;
      return this;
    }

    public Builder setMinErrorTransitionDelay(int minErrorTransitionDelay) {
      this.minErrorTransitionDelay = minErrorTransitionDelay;
      return this;
    }

    public Builder setShortTransitionDelay(int shortTransitionDelay) {
      this.shortTransitionDelay = shortTransitionDelay;
      return this;
    }

    public Builder setImmediateTransitionDelay(int immediateTransitionDelay) {
      this.immediateTransitionDelay = immediateTransitionDelay;
      return this;
    }

    public Builder setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    private int getIntegerProperty(Environment env, String key, int defaultValue) {
      if (env != null) {
        return env.getProperty(key, Integer.class, defaultValue);
      }
      return defaultValue;
    }

    public WorkflowSettings build() {
      return new WorkflowSettings(this);
    }
  }

  public DateTime getErrorTransitionActivation(int retryCount) {
    return now()
        .plusMillis(calculateBinaryBackoffDelay(retryCount + 1, minErrorTransitionDelay, maxErrorTransitionDelay));
  }

  protected int calculateBinaryBackoffDelay(int retryCount, int minDelay, int maxDelay) {
    return min(minDelay * (1 << retryCount), maxDelay);
  }

  public DateTime getShortTransitionActivation() {
    return now().plusMillis(shortTransitionDelay);
  }
}
