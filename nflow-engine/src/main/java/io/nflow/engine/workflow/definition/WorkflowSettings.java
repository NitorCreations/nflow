package io.nflow.engine.workflow.definition;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.DateTime.now;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

/**
 * Configuration for the workflow execution.
 */
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
public class WorkflowSettings extends ModelObject {
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
   * Unit is hours.
   */
  public final Integer historyDeletableAfterHours;
  /**
   * Condition to check if workflow instance history should be deleted (unless forced via StateExecution). Ignored if historyDeletableAfterHours is not set.
   * By default, returns true roughly every tenth time.
   */
  public final BooleanSupplier deleteHistoryCondition;

  WorkflowSettings(Builder builder) {
    this.minErrorTransitionDelay = builder.minErrorTransitionDelay;
    this.maxErrorTransitionDelay = builder.maxErrorTransitionDelay;
    this.shortTransitionDelay = builder.shortTransitionDelay;
    this.immediateTransitionDelay = builder.immediateTransitionDelay;
    this.maxRetries = builder.maxRetries;
    this.maxSubsequentStateExecutions = builder.maxSubsequentStateExecutions;
    this.maxSubsequentStateExecutionsPerState = new HashMap<>(builder.maxSubsequentStateExecutionsPerState);
    this.historyDeletableAfterHours = builder.historyDeletableAfterHours;
    this.deleteHistoryCondition = builder.deleteHistoryCondition;
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
    int maxSubsequentStateExecutions = 100;
    Map<WorkflowState, Integer> maxSubsequentStateExecutionsPerState = new HashMap<>();
    Integer historyDeletableAfterHours;
    Random rnd = new Random();
    BooleanSupplier deleteHistoryCondition = new BooleanSupplier() {

      @Override
      public boolean getAsBoolean() {
        return roughlyEveryTenthTime();
      }

      private boolean roughlyEveryTenthTime() {
        return rnd.nextInt(10) == 0;
      }

    };

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
     * Set the delay after which workflow history (actions, states) can be deleted from the database by nFlow.
     * The default value (<code>null</code>) indicates that history is not deletable.
     *
     * @param historyDeletableAfterHours
     *          Delay in hours.
     * @return this.
     */
    public Builder setHistoryDeletableAfterHours(Integer historyDeletableAfterHours) {
      this.historyDeletableAfterHours = historyDeletableAfterHours;
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
   * set. With default settings, returns true roughly every tenth time. To override, set deleteHistoryCondition.
   *
   * @return True if workflow instance history should be deleted.
   */
  public boolean deleteWorkflowInstanceHistory() {
    return deleteHistoryCondition.getAsBoolean();
  }

}
