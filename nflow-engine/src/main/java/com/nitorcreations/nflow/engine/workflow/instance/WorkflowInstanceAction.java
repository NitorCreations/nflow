package com.nitorcreations.nflow.engine.workflow.instance;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

/**
 * An execution of a workflow instance state.
 */
public class WorkflowInstanceAction {

  /**
   * The workflow identifier.
   */
  public final int workflowId;

  /**
   * The id for executor that processed this state.
   */
  public final int executorId;

  /**
   * The workflow state before the execution.
   */
  public final String state;

  /**
   * The description of the action taken in this state.
   */
  public final String stateText;

  /**
   * The retry attempt number. Zero when the state is executed for the first time.
   */
  public final int retryNo;

  /**
   * The start time of the execution of the state.
   */
  public final DateTime executionStart;

  /**
   * The end time of the execution of the state.
   */
  public final DateTime executionEnd;

  WorkflowInstanceAction(Builder builder) {
    this.workflowId = builder.workflowId;
    this.executorId = builder.executorId;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.retryNo = builder.retryNo;
    this.executionStart = builder.executionStart;
    this.executionEnd = builder.executionEnd;
  }

  /**
   * The builder for a workflow instance action.
   */
  public static class Builder {

    int workflowId;
    int executorId;
    String state;
    String stateText;
    int retryNo;
    DateTime executionStart;
    DateTime executionEnd;

    /**
     * Create a builder for a workflow instance action.
     */
    public Builder() {
    }

    /**
     * Create a builder for a workflow instance action based on an existing workflow instance.
     * @param instance The workflow instance for which the action is created.
     */
    public Builder(WorkflowInstance instance) {
      this.workflowId = instance.id;
      this.state = instance.state;
      this.retryNo = instance.retries;
      this.executionStart = now();
    }

    /**
     * Set the workflow identifier.
     * @param workflowId The workflow identifier.
     * @return this.
     */
    public Builder setWorkflowId(int workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    /**
     * Set the executor id.
     * @param executorId The executor id.
     * @return this
     */
    public Builder setExecutorId(Integer executorId) {
      this.executorId = executorId;
      return this;
    }
    /**
     * Set the state.
     * @param state The name of the state.
     * @return this.
     */
    public Builder setState(String state) {
      this.state = state;
      return this;
    }

    /**
     * Set the state text.
     * @param stateText The state text.
     * @return this.
     */
    public Builder setStateText(String stateText) {
      this.stateText = stateText;
      return this;
    }

    /**
     * Set the retry number.
     * @param retryNo The retry number.
     * @return this.
     */
    public Builder setRetryNo(int retryNo) {
      this.retryNo = retryNo;
      return this;
    }

    /**
     * Set the execution start time.
     * @param executionStart The execution start time.
     * @return this.
     */
    public Builder setExecutionStart(DateTime executionStart) {
      this.executionStart = executionStart;
      return this;
    }

    /**
     * Set the execution end time.
     * @param executionEnd The execution end time.
     * @return this.
     */
    public Builder setExecutionEnd(DateTime executionEnd) {
      this.executionEnd = executionEnd;
      return this;
    }

    /**
     * Build the workflow instance action.
     * @return The workflow instance action.
     */
    public WorkflowInstanceAction build() {
      return new WorkflowInstanceAction(this);
    }
  }
}
