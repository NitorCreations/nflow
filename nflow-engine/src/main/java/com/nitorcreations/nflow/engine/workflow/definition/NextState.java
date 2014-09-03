package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

public class NextState {

  private final boolean isFailure;
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

  public static NextState retryWithActivation(String reason, DateTime activation) {
    return new NextState(true, activation, null, reason);
  }

  public static NextState moveToStateWithActivation(WorkflowState nextState, DateTime activation, String reason) {
    return new NextState(false, activation, nextState, reason);
  }

  public static NextState moveToState(WorkflowState nextState, String reason) {
    return moveToStateWithActivation(nextState, now(), reason);
  }

  public static NextState stopInState(WorkflowState finalState, String reason) {
    return moveToStateWithActivation(finalState, null, reason);
  }
}
