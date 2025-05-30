package io.nflow.engine.workflow.definition;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.Duration.millis;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardMinutes;
import static org.joda.time.Duration.standardSeconds;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.exception.StateProcessExceptionHandling;
import io.nflow.engine.model.ModelObject;

/**
 * Configuration for the workflow execution.
 */
public class WorkflowSettings extends ModelObject {

  private static final BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> DEFAULT_EXCEPTION_ANALYZER = (state,
      thrown) -> new StateProcessExceptionHandling.Builder()
          .setRetryable(!thrown.getClass().isAnnotationPresent(NonRetryable.class)).build();

  private static final Logger logger = getLogger(WorkflowSettings.class);

  /**
   * Minimum delay on execution retry after an error. Unit is milliseconds.
   */
  public final long minErrorTransitionDelay;
  /**
   * Maximum delay on execution retry after an error. Unit is milliseconds.
   */
  public final long maxErrorTransitionDelay;
  /**
   * Length of forced delay to break execution of a step that is considered to be busy looping. Unit is milliseconds.
   */
  public final long shortTransitionDelay;
  /**
   * Maximum retry attempts.
   */
  public final int maxRetries;
  /**
   * Maximum number of subsequent state executions before forcing a short transition delay.
   */
  public final int maxSubsequentStateExecutions;
  /**
   * Maximum number of subsequent state executions before forcing a short transition delay, per state.
   */
  public final Map<WorkflowState, Integer> maxSubsequentStateExecutionsPerState;
  /**
   * Delay after which workflow instance history (actions, states) can be deleted from database by nFlow.
   */
  public final ReadablePeriod historyDeletableAfter;
  /**
   * Condition to check if workflow instance history should be deleted (unless forced via StateExecution). Ignored if historyDeletableAfterHours is not set.
   * By default, returns true roughly every tenth time.
   */
  public final BooleanSupplier deleteHistoryCondition;
  /**
   * Default priority for new workflow instances.
   */
  public final short defaultPriority;

  private final BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> exceptionAnalyzer;

  WorkflowSettings(Builder builder) {
    this.minErrorTransitionDelay = builder.minErrorTransitionDelay.getMillis();
    this.maxErrorTransitionDelay = builder.maxErrorTransitionDelay.getMillis();
    this.shortTransitionDelay = builder.shortTransitionDelay.getMillis();
    this.maxRetries = builder.maxRetries;
    this.maxSubsequentStateExecutions = builder.maxSubsequentStateExecutions;
    this.maxSubsequentStateExecutionsPerState = new HashMap<>(builder.maxSubsequentStateExecutionsPerState);
    this.historyDeletableAfter = builder.historyDeletableAfter;
    this.deleteHistoryCondition = builder.deleteHistoryCondition;
    this.defaultPriority = builder.defaultPriority;
    this.exceptionAnalyzer = builder.exceptionAnalyzer;
  }

  /**
   * Builder for workflow settings.
   */
  public static class Builder {

    ReadableDuration maxErrorTransitionDelay = standardDays(1);
    ReadableDuration minErrorTransitionDelay = standardMinutes(1);
    ReadableDuration shortTransitionDelay = standardSeconds(30);
    int maxRetries = 17;
    int maxSubsequentStateExecutions = 100;
    Map<WorkflowState, Integer> maxSubsequentStateExecutionsPerState = new HashMap<>();
    ReadablePeriod historyDeletableAfter = Period.days(45);
    short defaultPriority = 0;
    BooleanSupplier deleteHistoryCondition = onAverageEveryNthExecution(100);
    BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> exceptionAnalyzer;

    /**
     * Returns true randomly every n:th time.
     *
     * @param n
     *          Controls the frequency of returning true. With n=1, returns true every time. With n=100, returns true on average
     *          once per 100 calls.
     * @return Producer of boolean values
     */
    public static BooleanSupplier onAverageEveryNthExecution(int n) {
      return () -> ThreadLocalRandom.current().nextInt(n) == 0;
    }

    /**
     * Returns true once per day (during early hours).
     *
     * @return Producer of boolean values
     */
    public static BooleanSupplier oncePerDay() {
      // minutes and seconds vary by start time between nodes
      AtomicLong nextExecution = new AtomicLong(LocalDateTime.now().plusDays(1).withHourOfDay(4).toDateTime().getMillis());
      return () -> {
        long now = currentTimeMillis();
        long next = nextExecution.get();
        if (now > next) {
          nextExecution.set(next + standardDays(1).getMillis());
          return true;
        }
        return false;
      };
    }

    /**
     * Set the maximum delay on execution retry after an error.
     *
     * @param maxErrorTransitionDelay
     *          The delay.
     * @return this.
     */
    public Builder setMaxErrorTransitionDelay(Duration maxErrorTransitionDelay) {
      this.maxErrorTransitionDelay = maxErrorTransitionDelay;
      return this;
    }

    /**
     * Set the minimum delay on execution retry after an error.
     *
     * @param minErrorTransitionDelay
     *          The delay.
     * @return this.
     */
    public Builder setMinErrorTransitionDelay(Duration minErrorTransitionDelay) {
      this.minErrorTransitionDelay = minErrorTransitionDelay;
      return this;
    }

    /**
     * Set the length of forced delay to break execution of a step that is considered to be busy looping.
     *
     * @param shortTransitionDelay
     *          The delay.
     * @return this.
     */
    public Builder setShortTransitionDelay(Duration shortTransitionDelay) {
      this.shortTransitionDelay = shortTransitionDelay;
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
     * Set maximum number of subsequent state executions before forcing a short transition delay.
     *
     * @param maxSubsequentStateExecutions
     *          Maximum number of subsequent state executions.
     * @return this.
     */
    public Builder setMaxSubsequentStateExecutions(int maxSubsequentStateExecutions) {
      this.maxSubsequentStateExecutions = maxSubsequentStateExecutions;
      return this;
    }

    /**
     * Set maximum number of subsequent state executions before forcing a short transition delay for given state.
     *
     * @param state
     *          The state for which the limit is applied.
     * @param maxSubsequentStateExecutions
     *          Maximum number of subsequent state executions.
     * @return this.
     */
    public Builder setMaxSubsequentStateExecutions(WorkflowState state, int maxSubsequentStateExecutions) {
      this.maxSubsequentStateExecutionsPerState.put(state, maxSubsequentStateExecutions);
      return this;
    }

    /**
     * Set the delay after which workflow history (actions, states) can be deleted from the database by nFlow. Setting value to
     * <code>null</code> indicates that history is not deletable. Default is 45 days.
     *
     * @param period
     *          Delay after which history can be deleted.
     * @return this.
     */
    public Builder setHistoryDeletableAfter(ReadablePeriod period) {
      this.historyDeletableAfter = period;
      return this;
    }

    /**
     * Set the condition to be checked to decide if workflow instance history should be deleted.
     *
     * @param deleteHistoryCondition
     *          Function to be called.
     * @return this.
     */
    public Builder setDeleteHistoryCondition(BooleanSupplier deleteHistoryCondition) {
      this.deleteHistoryCondition = deleteHistoryCondition;
      return this;
    }

    /**
     * Set the default priority for new workflow instances.
     *
     * @param defaultPriority
     *          Default priority.
     * @return this.
     */
    public Builder setDefaultPriority(short defaultPriority) {
      this.defaultPriority = defaultPriority;
      return this;
    }

    /**
     * Set the exception analyzer function.
     *
     * @param exceptionAnalyzer
     *          The exception analyzer function.
     * @return this.
     */
    public Builder setExceptionAnalyzer(BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> exceptionAnalyzer) {
      this.exceptionAnalyzer = exceptionAnalyzer;
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
    try {
      return max(minDelay, min(delay.longValueExact(), maxDelay));
    } catch (@SuppressWarnings("unused") ArithmeticException overflow) {
      return maxDelay;
    }
  }

  /**
   * Return the delay before next activation after detecting a busy loop.
   *
   * @return The delay in milliseconds.
   */
  public DateTime getShortTransitionActivation() {
    return now().plus(millis(shortTransitionDelay));
  }

  /**
   * Return the maximum number of subsequent state executions before forcing a short transition delay.
   * @param state The state for which the limit is checked.
   *
   * @return The maximum number of subsequent state executions.
   */
  public int getMaxSubsequentStateExecutions(WorkflowState state) {
    return maxSubsequentStateExecutionsPerState.getOrDefault(state, maxSubsequentStateExecutions);
  }

  /**
   * Return true if workflow instance history should be deleted. Called by WorkflowStateProcessor after processing a state if historyDeletableAfterHours is
   * set. With default settings, returns true roughly every hundredth time. To override, set deleteHistoryCondition.
   *
   * @return True if workflow instance history should be deleted.
   */
  public boolean deleteWorkflowInstanceHistory() {
    return deleteHistoryCondition.getAsBoolean();
  }

  /**
   * Return default priority for new workflow instances.
   *
   * @return Default priority for new workflow instances.
   */
  public Short getDefaultPriority() {
    return defaultPriority;
  }

  /**
   * Analyze exception thrown by a state method to determine how it should be handled.
   *
   * @param state
   *          The state that failed to be processed.
   * @param thrown
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  public StateProcessExceptionHandling analyzeExeption(WorkflowState state, Throwable thrown) {
    if (exceptionAnalyzer != null) {
      try {
        return exceptionAnalyzer.apply(state, thrown);
      } catch (Exception e) {
        logger.error("Custom exception analysis failed, using default analyzer.", e);
      }
    }
    return DEFAULT_EXCEPTION_ANALYZER.apply(state, thrown);
  }
}
