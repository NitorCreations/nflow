package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

public class NextAction {

  private final boolean isFailure;
  private final DateTime activation;
  private final WorkflowState nextState;
  private final String reason;
  private final boolean isSaveTrace = true;

  private NextAction(boolean isFailure, DateTime activation, WorkflowState nextState, String reason) {
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

  /**
   * Schedule retry for current state at time {@code activation}.
   */
  public static NextAction retryAt(DateTime activation, String reason) {
    return new NextAction(true, activation, null, reason);
  }

  /**
   * Schedule processing of state {@code nextState} at time {@code activation}.
   */
  public static NextAction moveToStateWithActivation(WorkflowState nextState, DateTime activation, String reason) {
    return new NextAction(false, activation, nextState, reason);
  }

  /**
   * Schedule processing of state {@code nextState} immediately.
   */
  public static NextAction moveToState(WorkflowState nextState, String reason) {
    return moveToStateWithActivation(nextState, now(), reason);
  }

  /**
   * Set next state to {@finalState} and do not schedule its
   * processing. Used to indicate final state of workflow or a manual state.
   */
  public static NextAction stopInState(WorkflowState finalState, String reason) {
    return moveToStateWithActivation(finalState, null, reason);
  }
}
