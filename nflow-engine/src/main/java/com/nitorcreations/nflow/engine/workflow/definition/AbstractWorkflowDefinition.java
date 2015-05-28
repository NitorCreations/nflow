package com.nitorcreations.nflow.engine.workflow.definition;

import static java.lang.String.format;
import static org.joda.time.DateTime.now;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.joda.time.DateTime;
import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * The base class for all workflow definitions.
 */
public abstract class AbstractWorkflowDefinition<S extends WorkflowState> {

  private final String type;
  private String name;
  private String description;
  private final S initialState;
  private final S errorState;
  private final WorkflowSettings settings;
  protected final Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
  protected final Map<String, WorkflowState> failureTransitions = new LinkedHashMap<>();
  private Map<String, WorkflowStateMethod> stateMethods;

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState) {
    this(type, initialState, errorState, new WorkflowSettings.Builder().build());
  }

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    this(type, initialState, errorState, settings, null);
  }

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings,
      Map<String, WorkflowStateMethod> stateMethods) {
    Assert.notNull(initialState, "initialState must not be null");
    Assert.isTrue(initialState.getType() == WorkflowStateType.start, "initialState must be a start state");
    Assert.notNull(errorState, "errorState must not be null");
    Assert.isTrue(errorState.getType().isFinal(), "errorState must be a final state");
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
    this.settings = settings;
    if (stateMethods != null) {
      this.stateMethods = stateMethods;
    } else {
      this.stateMethods = new WorkflowDefinitionScanner().getStateMethods(getClass());
    }
    requireStateMethodExists(initialState);
  }

  /**
   * Return the name of the workflow.
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the workflow.
   * @param name The name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the description of the workflow.
   * @return The description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description of the workflow.
   * @param description The description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Return the type of the workflow.
   * @return The type.
   */
  public String getType() {
    return type;
  }

  /**
   * Return the initial state of the workflow.
   * @return Workflow state.
   */
  public S getInitialState() {
    return initialState;
  }

  /**
   * Return the generic error state of the workflow.
   * @return Workflow state.
   */
  public S getErrorState() {
    return errorState;
  }

  /**
   * Return all possible states of the workflow.
   * @return Set of workflow states.
   */
  public abstract Set<S> getStates();

  /**
   * Return allowed transitions between the states of the workflow.
   * @return Map from origin states to a list of target states.
   */
  public Map<String, List<String>> getAllowedTransitions() {
    return new LinkedHashMap<>(allowedTransitions);
  }

  /**
   * Return transitions from states to failure states.
   * @return Map from origin state to failure state.
   */
  public Map<String, WorkflowState> getFailureTransitions() {
    return new LinkedHashMap<>(failureTransitions);
  }

  /**
   * Add a state transition to the allowed transitions.
   * @param originState The origin state.
   * @param targetState The target state.
   * @return This.
   */
  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitionsFor(originState).add(targetState.name());
    return this;
  }

  /**
   * Add a state and failure state transitions to the allowed transitions.
   * @param originState The origin state.
   * @param targetState The target state.
   * @param failureState The failure state.
   * @return This.
   */
  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    Assert.notNull(failureState, "Failure state can not be null");
    requireStateMethodExists(failureState);
    WorkflowState existingFailure = failureTransitions.put(originState.name(), failureState);
    Assert.isTrue(existingFailure == null || existingFailure.equals(failureState), "Different failureState '" + existingFailure
        + "' already defined for originState '" + originState.name() + "'");
    return permit(originState, targetState);
  }

  private List<String> allowedTransitionsFor(S state) {
    if (!allowedTransitions.containsKey(state.name())) {
      allowedTransitions.put(state.name(), new ArrayList<String>());
    }
    return allowedTransitions.get(state.name());
  }

  /**
   * Return the workflow settings.
   * @return Workflow settings.
   */
  public WorkflowSettings getSettings() {
    return settings;
  }

  void requireStateMethodExists(S state) {
    WorkflowStateMethod stateMethod = stateMethods.get(state.name());
    if (stateMethod == null && !state.getType().isFinal()) {
      String msg = format(
          "Class '%s' is missing non-final state handling method 'public NextAction %s(StateExecution execution, ... args)'",
          this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
    if (stateMethod != null) {
      WorkflowStateType stateType = state.getType();
      Class<?> returnType = stateMethod.method.getReturnType();
      if (!stateType.isFinal() && !NextAction.class.equals(returnType)) {
        String msg = format("Class '%s' has a non-final state method '%s' that does not return NextAction", this.getClass()
            .getName(), state.name());
        throw new IllegalArgumentException(msg);
      }
      if (stateType.isFinal() && !Void.TYPE.equals(returnType)) {
        String msg = format("Class '%s' has a final state method '%s' that returns a value", this.getClass().getName(),
            state.name());
        throw new IllegalArgumentException(msg);
      }
    }
  }

  /**
   * Handle retries for the state execution. The default implementation moves
   * the workflow to a failure state after the maximum retry attempts is
   * exceeded. If there is no failure state defined for the retried state, moves
   * the workflow to the generic error state and stops processing. Error state
   * handler method, if it exists, is not executed. If the maximum retry attempts
   * is not exceeded, schedules the next attempt for the state based on workflow
   * settings. This method is called when an unexpected exception happens during
   * state method handling.
   * @param execution State execution information.
   */
  public void handleRetry(StateExecutionImpl execution) {
    handleRetryAfter(execution, getSettings().getErrorTransitionActivation(execution.getRetries()));
  }

  /**
   * Handle retries for the state execution. The default implementation moves
   * the workflow to a failure state after the maximum retry attempts is
   * exceeded. If there is no failure state defined for the retried state, moves
   * the workflow to the generic error state and stops processing. Error state
   * handler method, if it exists, is not executed. If the maximum retry attempts
   * is not exceeded, schedules the next attempt to the given activation time.
   * This method is called when a retry attempt is explicitly requested by a
   * state handling method.
   * @param execution State execution information.
   * @param activation Time for next retry attempt.
   */
  public void handleRetryAfter(StateExecutionImpl execution, DateTime activation) {
    if (execution.getRetries() >= getSettings().maxRetries) {
      execution.setRetry(false);
      execution.setRetryCountExceeded();
      WorkflowState failureState = failureTransitions.get(execution.getCurrentStateName());
      WorkflowState currentState = getState(execution.getCurrentStateName());
      if (failureState != null) {
        execution.setNextState(failureState);
        execution.setNextStateReason("Max retry count exceeded, going to failure state");
        execution.setNextActivation(now());
      } else {
        execution.setNextState(errorState);
        if (errorState.equals(currentState)) {
          execution.setNextStateReason("Max retry count exceeded when handling error state, processing stopped");
          execution.setNextActivation(null);
        } else {
          execution.setNextStateReason("Max retry count exceeded, no failure state defined, going to error state");
          execution.setNextActivation(now());
        }
      }
    } else {
      execution.setNextActivation(activation);
    }
  }

  /**
   * Returns the workflow state method for the given state name.
   * @param stateName The name of the workflow state.
   * @return The workflow state method, or null if not found.
   */
  public WorkflowStateMethod getMethod(String stateName) {
    return stateMethods.get(stateName);
  }

  /**
   * Returns the workflow state for the given state name.
   * @param state The name of the workflow state.
   * @return The workflos state matching the state name.
   * @throws IllegalStateException when a matching state can not be found.
   */
  public WorkflowState getState(String state) {
    for (WorkflowState s : getStates()) {
      if (Objects.equals(s.name(), state)) {
        return s;
      }
    }
    throw new IllegalStateException("No state '" + state + "' in workflow definiton " + getType());
  }

  /**
   * Check if the given state is a valid start state.
   * @param state The name of the workflow state.
   * @return True if the given state is a valid start date, false otherwise.
   * @throws IllegalStateException if the given state name does not match any state.
   */
  public boolean isStartState(String state) {
    return getState(state).getType() == WorkflowStateType.start;
  }

  /**
   * Return true if the given nextAction is permitted for given instance.
   * @param instance The workflow instance for which the action is checked.
   * @param nextAction The action to be checked.
   * @return True if the nextAction is permitted, false otherwise.
   */
  public boolean isAllowedNextAction(WorkflowInstance instance, NextAction nextAction) {
    if (nextAction.isRetry()) {
      return true;
    }
    List<String> allowedNextStates = allowedTransitions.get(instance.state);
    if (allowedNextStates != null && allowedNextStates.contains(nextAction.getNextState().name())) {
      return true;
    }
    if (nextAction.getNextState() == failureTransitions.get(instance.state)) {
      return true;
    }
    return nextAction.getNextState() == getErrorState();
  }
}
