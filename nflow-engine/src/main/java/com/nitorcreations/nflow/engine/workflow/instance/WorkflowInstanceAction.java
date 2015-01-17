package com.nitorcreations.nflow.engine.workflow.instance;

import static java.util.Collections.unmodifiableMap;
import static org.joda.time.DateTime.now;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;

/**
 * An execution of a workflow instance state.
 */
public class WorkflowInstanceAction {
  /**
   * Describes the trigger for the action.
   */
  public enum WorkflowActionType {
    /**
     * Normal state execution.
     */
    stateExecution,
    /**
     * Normal state execution that resulted in failure.
     */
    stateExecutionFailed,
    /**
     * External change to the workflow instance.
     */
    externalChange,
    /**
     * Dead executor recovery.
     */
    recovery
  }

  /**
   * The workflow instance identifier.
   * @deprecated Use @{code workflowInstanceId} instead. This will be removed in 2.0.0.
   */
  @Deprecated
  public final int workflowId;

  /**
   * The workflow instance identifier.
   */
  public final int workflowInstanceId;

  /**
   * The id for executor that processed this state.
   */
  public final int executorId;

  /**
   * The type of action.
   */
  public final WorkflowActionType type;

  /**
   * The workflow state before the execution.
   */
  public final String state;

  /**
   * The description of the action taken in this state.
   */
  public final String stateText;

  /**
   * State variables that were updated in this state.
   */
  public final Map<String, String> updatedStateVariables;

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
    this.workflowId = builder.workflowInstanceId;
    this.workflowInstanceId = builder.workflowInstanceId;
    this.executorId = builder.executorId;
    this.type = builder.type;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.updatedStateVariables = unmodifiableMap(builder.updatedStateVariables);
    this.retryNo = builder.retryNo;
    this.executionStart = builder.executionStart;
    this.executionEnd = builder.executionEnd;
  }

  /**
   * The builder for a workflow instance action.
   */
  public static class Builder {

    int workflowInstanceId;
    int executorId;
    WorkflowActionType type;
    String state;
    String stateText;
    int retryNo;
    DateTime executionStart;
    DateTime executionEnd;
    Map<String, String> updatedStateVariables = new LinkedHashMap<>();

    /**
     * Create a builder for a workflow instance action.
     */
    public Builder() {
    }

    /**
     * Create a builder for a workflow instance action based on an existing workflow instance action.
     * @param action The workflow instance action to be copied.
     */
    public Builder(WorkflowInstanceAction action) {
      this.executionEnd = action.executionEnd;
      this.executionStart = action.executionStart;
      this.executorId = action.executorId;
      this.retryNo = action.retryNo;
      this.type = action.type;
      this.state = action.state;
      this.stateText = action.stateText;
      this.updatedStateVariables.putAll(action.updatedStateVariables);
      this.workflowInstanceId = action.workflowInstanceId;
    }

    /**
     * Create a builder for a workflow instance action based on an existing workflow instance.
     * @param instance The workflow instance for which the action is created.
     */
    public Builder(WorkflowInstance instance) {
      this.workflowInstanceId = instance.id;
      this.state = instance.state;
      this.retryNo = instance.retries;
      this.executionStart = now();
    }

    /**
     * Set the workflow instance identifier.
     * @param workflowInstanceId The workflow instance identifier.
     * @return this.
     * @deprecated Use @{code setWorkflowInstanceId} instead. This will be removed in 2.0.0.
     */
    @Deprecated
    public Builder setWorkflowId(int workflowInstanceId) {
      this.workflowInstanceId = workflowInstanceId;
      return this;
    }

    /**
     * Set the workflow instance identifier.
     * @param workflowInstanceId The workflow instance identifier.
     * @return this.
     */
    public Builder setWorkflowInstanceId(int workflowInstanceId) {
      this.workflowInstanceId = workflowInstanceId;
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
     * Set the trigger type of the action.
     * @param actionType The action type.
     * @return this
     */
    public Builder setType(WorkflowActionType actionType) {
      this.type = actionType;
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
     * Set the updated state variables.
     * @param updatedStateVariables The updated state variables.
     * @return this.
     */
    public Builder setUpdatedStateVariables(Map<String, String> updatedStateVariables) {
      this.updatedStateVariables = updatedStateVariables;
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
      if (type == null) {
        throw new IllegalStateException("Missing type");
      }
      return new WorkflowInstanceAction(this);
    }
  }
}
