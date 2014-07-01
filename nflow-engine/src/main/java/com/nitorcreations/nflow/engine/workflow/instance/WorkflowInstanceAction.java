package com.nitorcreations.nflow.engine.workflow.instance;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

public class WorkflowInstanceAction {

  public final int workflowId;
  public final String state;
  public final String stateText;
  public final int retryNo;
  public final DateTime executionStart;
  public final DateTime executionEnd;

  public WorkflowInstanceAction(Builder builder) {
    this.workflowId = builder.workflowId;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.retryNo = builder.retryNo;
    this.executionStart = builder.executionStart;
    this.executionEnd = builder.executionEnd;
  }

  public static class Builder {

    int workflowId;
    String state;
    String stateText;
    int retryNo;
    DateTime executionStart;
    DateTime executionEnd;

    public Builder() {
    }

    public Builder(WorkflowInstance instance) {
      this.workflowId = instance.id;
      this.state = instance.state;
      this.retryNo = instance.retries;
      this.executionStart = now();
    }

    public Builder setWorkflowId(int workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public Builder setState(String state) {
      this.state = state;
      return this;
    }

    public Builder setStateText(String stateText) {
      this.stateText = stateText;
      return this;
    }

    public Builder setRetryNo(int retryNo) {
      this.retryNo = retryNo;
      return this;
    }

    public Builder setExecutionStart(DateTime executionStart) {
      this.executionStart = executionStart;
      return this;
    }

    public Builder setExecutionEnd(DateTime executionEnd) {
      this.executionEnd = executionEnd;
      return this;
    }

    public WorkflowInstanceAction build() {
      return new WorkflowInstanceAction(this);
    }

  }

}
