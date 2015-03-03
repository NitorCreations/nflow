package com.nitorcreations.nflow.engine.workflow.definition;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.DateTime.now;

import java.math.BigInteger;

import org.joda.time.DateTime;

/**
 * Configuration for the workflow execution.
 */
public class WorkflowSettings {
  /**
   * Minimum delay on execution retry after an error. Unit is milliseconds.
   */
  public final int minErrorTransitionDelay;
  /**
   * Maximum delay on execution retry after an error. Unit is milliseconds.
   */
  public final int maxErrorTransitionDelay;
  /**
   * Length of forced delay to break execution of a step that is considered to be busy looping. Unit is milliseconds.
   */
  public final int shortTransitionDelay;
  /**
   * Immediate transition delay.
   */
  public final int immediateTransitionDelay;
  /**
   * Maximum retry attempts.
   */
  public final int maxRetries;

  WorkflowSettings(Builder builder) {
    this.minErrorTransitionDelay = builder.minErrorTransitionDelay;
    this.maxErrorTransitionDelay = builder.maxErrorTransitionDelay;
    this.shortTransitionDelay = builder.shortTransitionDelay;
    this.immediateTransitionDelay = builder.immediateTransitionDelay;
    this.maxRetries = builder.maxRetries;
  }

  /**
   * Builder for workflow settings.
   */
  public static class Builder {

    int maxErrorTransitionDelay = (int) DAYS.toMillis(1);
    int minErrorTransitionDelay = (int) MINUTES.toMillis(1);
    int shortTransitionDelay = (int) SECONDS.toMillis(30);
    int immediateTransitionDelay = 0;
    int maxRetries = 17;

    /**
     * Set the maximum delay on execution retry after an error.
     *
     * @param maxErrorTransitionDelay
     *          Delay in milliseconds.
     * @return this.
     */
    public Builder setMaxErrorTransitionDelay(int maxErrorTransitionDelay) {
      this.maxErrorTransitionDelay = maxErrorTransitionDelay;
      return this;
    }

    /**
     * Set the minimum delay on execution retry after an error.
     *
     * @param minErrorTransitionDelay
     *          Delay in milliseconds.
     * @return this.
     */
    public Builder setMinErrorTransitionDelay(int minErrorTransitionDelay) {
      this.minErrorTransitionDelay = minErrorTransitionDelay;
      return this;
    }

    /**
     * Set the length of forced delay to break execution of a step that is considered to be busy looping.
     *
     * @param shortTransitionDelay
     *          Delay in milliseconds.
     * @return this.
     */
    public Builder setShortTransitionDelay(int shortTransitionDelay) {
      this.shortTransitionDelay = shortTransitionDelay;
      return this;
    }

    /**
     * Set immediate transition delay.
     *
     * @param immediateTransitionDelay
     *          Delay in milliseconds.
     * @return this.
     */
    public Builder setImmediateTransitionDelay(int immediateTransitionDelay) {
      this.immediateTransitionDelay = immediateTransitionDelay;
      return this;
    }

    /**
     * Set maximum retry attempts.
     *
     * @param maxRetries
     *          Maximum number of retries.
     * @return this.
     */
    public Builder setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Create workflow settings object.
     *
     * @return Workflow settings.
     */
    public WorkflowSettings build() {
      return new WorkflowSettings(this);
    }
  }

  /**
   * Return next activation time after error.
   *
   * @param retryCount
   *          Number of retry attemps.
   * @return Next activation time.
   */
  public DateTime getErrorTransitionActivation(int retryCount) {
    return now().plus(calculateBinaryBackoffDelay(retryCount + 1, minErrorTransitionDelay, maxErrorTransitionDelay));
  }

  /**
   * Return activation delay based on retry attempt number.
   *
   * @param retryCount
   *          Retry attempt number.
   * @param minDelay
   *          Minimum retry delay.
   * @param maxDelay
   *          Maximum retry delay.
   * @return Delay in milliseconds.
   */
  protected long calculateBinaryBackoffDelay(int retryCount, long minDelay, long maxDelay) {
    BigInteger delay = BigInteger.valueOf(minDelay).multiply(BigInteger.valueOf(2).pow(retryCount));
    if(!BigInteger.valueOf(delay.longValue()).equals(delay)) {
      // got overflow in delay calculation
      // Java 1.8 has delay.longValueExact()
      return maxDelay;
    }
    return max(minDelay, min(delay.longValue(), maxDelay));
  }

  /**
   * Return the delay before next activation after detecting a busy loop.
   *
   * @return The delay in milliseconds.
   */
  public DateTime getShortTransitionActivation() {
    return now().plusMillis(shortTransitionDelay);
  }
}
