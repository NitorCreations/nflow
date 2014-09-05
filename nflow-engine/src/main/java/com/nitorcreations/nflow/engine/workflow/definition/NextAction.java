package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.internal.executor.InvalidNextActionException;

public class NextAction {

  private final DateTime activation;
  private final WorkflowState nextState;
  private final String reason;
  private final boolean isSaveTrace = true;

  private NextAction(DateTime activation, WorkflowState nextState, String reason) {
    this.reason = reason;
    this.nextState = nextState;
    this.activation = activation;
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
  public static NextAction retryAfter(DateTime activation, String reason) {
    assertNotNull(activation, "Activation can not be null");
    return new NextAction(activation, null, reason);
  }

  /**
   * Schedule processing of state {@code nextState} at time {@code activation}.
   */
  public static NextAction moveToStateAfter(WorkflowState nextState, DateTime activation, String reason) {
    assertNotNull(nextState, "Next state can not be null");
    assertNotNull(activation, "Activation can not be null");
    return new NextAction(activation, nextState, reason);
  }

  /**
   * Schedule processing of state {@code nextState} immediately.
   */
  public static NextAction moveToState(WorkflowState nextState, String reason) {
    assertNotNull(nextState, "Next state can not be null");
    return new NextAction(now(), nextState, reason);
  }

  /**
   * Set next state to {@finalState} and do not schedule its
   * processing. Used to indicate final state of workflow or a manual state.
   */
  public static NextAction stopInState(WorkflowState finalState, String reason) {
    assertNotNull(finalState, "Final state can not be null");
    return new NextAction(null, finalState, reason);
  }

  private static void assertNotNull(Object object, String message) {
    if (object == null) {
      throw new InvalidNextActionException(message);
    }
  }
}
