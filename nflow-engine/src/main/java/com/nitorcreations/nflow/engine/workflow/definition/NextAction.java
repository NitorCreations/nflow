package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.internal.executor.InvalidNextActionException;

public class NextAction {

  private final DateTime activation;
  private final WorkflowState nextState;
  private final String reason;

  private NextAction(DateTime activation, WorkflowState nextState, String reason) {
    this.reason = reason;
    this.nextState = nextState;
    this.activation = activation;
  }

  /**
   * Return the time after which the workflow should be activated. There is no
   * guarantee that the workflow will be activated exactly at the given time.
   * @return The activation time.
   */
  public DateTime getActivation() {
    return activation;
  }

  /**
   * Return the next state of the workflow.
   * @return The workflow state.
   */
  public WorkflowState getNextState() {
    return nextState;
  }

  /**
   * Return the reason for the next action.
   * @return The reason.
   */
  public String getReason() {
    return reason;
  }

  /**
   * Schedule retry for current state at time {@code activation}.
   * @param activation The time after which the workflow can be activated.
   * @param reason The reason for the action.
   * @return A valid {@code NextAction} value.
   */
  public static NextAction retryAfter(DateTime activation, String reason) {
    assertNotNull(activation, "Activation can not be null");
    return new NextAction(activation, null, reason);
  }

  /**
   * Schedule processing of state {@code nextState} at time {@code activation}.
   * @param nextState The next workflow state.
   * @param activation The time after which the workflow can be activated.
   * @param reason The reason for the action.
   * @return A valid {@code NextAction} value.
   */
  public static NextAction moveToStateAfter(WorkflowState nextState, DateTime activation, String reason) {
    assertNotNull(nextState, "Next state can not be null");
    assertNotNull(activation, "Activation can not be null");
    return new NextAction(activation, nextState, reason);
  }

  /**
   * Schedule processing of state {@code nextState} immediately.
   * @param nextState The next workflow state.
   * @param reason The reason for the action.
   * @return A valid {@code NextAction} value.
   */
  public static NextAction moveToState(WorkflowState nextState, String reason) {
    assertNotNull(nextState, "Next state can not be null");
    return new NextAction(now(), nextState, reason);
  }

  /**
   * Set next state to {@code finalState} and do not schedule its
   * processing. The {@code finalState} workflow state type must
   * be final, e.g. it must be either end state or manual state.
   * The state handler method of the state, even if it exists, is
   * not executed. Additionally, there is no workflow action
   * recorded for the {@code finalState}. If you want to execute
   * the state handler method and record a workflow action, use
   * one of the {@code moveToState} methods instead.
   * @param finalState Final workflow state (end state or manual state).
   * @param reason The reason for the action.
   * @return A valid {@code NextAction} value.
   */
  public static NextAction stopInState(WorkflowState finalState, String reason) {
    assertNotNull(finalState, "State can not be null");
    assertFinalState(finalState);
    return new NextAction(null, finalState, reason);
  }

  private static void assertFinalState(WorkflowState state) {
    if (!state.getType().isFinal()) {
      throw new InvalidNextActionException("Cannot stop in a state that is not final");
    }
  }

  private static void assertNotNull(Object object, String message) {
    if (object == null) {
      throw new InvalidNextActionException(message);
    }
  }

  /**
   * Return true if this action is a retry of the current state.
   * @return True if action is a retry, false otherwise.
   */
  public boolean isRetry() {
    return nextState == null;
  }

}
