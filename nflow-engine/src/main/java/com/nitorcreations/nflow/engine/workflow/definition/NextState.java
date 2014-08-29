package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

public class NextState {

  private boolean isFailure;
  private final DateTime activation;
  private final WorkflowState nextState;
  private final String reason;
  private final boolean isSaveTrace = true;

  private NextState(boolean isFailure, DateTime activation, WorkflowState nextState, String reason) {
    this.reason = reason;
    this.nextState = nextState;
    this.activation = activation;
    this.isFailure = isFailure;
  }

  public boolean isFailure() {
    return isFailure;
  }

  public void setFailure(boolean isFailure) {
    this.isFailure = isFailure;
  }

  public DateTime getActivation() {
    return activation;
  }

  public WorkflowState getNextState() {
    return nextState;
  }

  public String getReason() {
    return reason;
  }

  public boolean isSaveTrace() {
    return isSaveTrace;
  }

  public static NextState nextStateWithActivation(WorkflowState nextState, DateTime activation, String reason) {
    return new NextState(false, activation, nextState, reason);
  }

  public static NextState moveToStateImmediately(WorkflowState nextState, String reason) {
    return nextStateWithActivation(nextState, now(), reason);
  }

  public static NextState stopInState(WorkflowState finalState, String reason) {
    return nextStateWithActivation(finalState, null, reason);
  }
}
