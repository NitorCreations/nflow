package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;

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
    this(type, initialState, errorState, new WorkflowSettings(null));
  }

  protected AbstractWorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    Assert.notNull(initialState, "initialState must not be null");
    Assert.notNull(errorState, "errorState must not be null");
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
    this.settings = settings;
    this.stateMethods = new WorkflowDefinitionScanner().getStateMethods(getClass());
    requireStateMethodExists(initialState);
  }

  /**
   * Return the name of the workflow.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the workflow.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the description of the workflow.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description of the workflow.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Return the type of the workflow.
   */
  public String getType() {
    return type;
  }

  /**
   * Return the initial state of the workflow.
   */
  public S getInitialState() {
    return initialState;
  }

  /**
   * Return the generic error state of the workflow.
   */
  public S getErrorState() {
    return errorState;
  }

  /**
   * Return all possible states of the workflow.
   */
  public abstract Set<S> getStates();

  /**
   * Return allowed transitions between the states of the workflow.
   */
  public Map<String, List<String>> getAllowedTransitions() {
    return new LinkedHashMap<>(allowedTransitions);
  }

  /**
   * Return allowed failure transitions between the states of the workflow.
   */
  public Map<String, WorkflowState> getFailureTransitions() {
    return new LinkedHashMap<>(failureTransitions);
  }

  /**
   * Add a state transition to the allowed transitions.
   *
   * @return This.
   */
  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState) {
    return permit(originState, targetState, null);
  }

  /**
   * Add a failure transition to the allowed failure transitions.
   *
   * @return This.
   */
  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitionsFor(originState).add(targetState.name());
    if (failureState != null) {
      requireStateMethodExists(failureState);
      failureTransitions.put(originState.name(), failureState);
    }
    return this;
  }

  private List<String> allowedTransitionsFor(S state) {
    if (!allowedTransitions.containsKey(state.name())) {
      allowedTransitions.put(state.name(), new ArrayList<String>());
    }
    return allowedTransitions.get(state.name());
  }

  /**
   * Return the workflow settings.
   */
  public WorkflowSettings getSettings() {
    return settings;
  }

  boolean isStateMethodObligatory(S state) {
    return state.getType() != manual && state.getType() != end;
  }

  void requireStateMethodExists(S state) {
    if (!stateMethods.containsKey(state.name()) && isStateMethodObligatory(state)) {
      String msg = format(
          "Class '%s' is missing state handling method 'public NextAction %s(StateExecution execution, ... args)'", this
              .getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
  }

  /**
   * Handle retries for the state execution. The default implementation moves
   * the workflow to a failure state after the maximum retry attempts is
   * exceeded. If there is no failure state defined for the retried state, moves
   * the workflow to the generic error state. If the maximum retry attempts is
   * not exceeded, schedules the next attempt for the state based on workflow
   * settings.
   */
  public void handleRetry(StateExecutionImpl execution) {
    if (execution.getRetries() >= getSettings().getMaxRetries()) {
      execution.setRetry(false);
      WorkflowState failureState = failureTransitions.get(execution.getCurrentStateName());
      if (failureState != null) {
        execution.setNextState(failureState);
        execution.setNextStateReason("Max retry count exceeded");
        execution.setNextActivation(getSettings().getErrorTransitionActivation());
      } else {
        execution.setNextState(errorState);
        execution.setNextStateReason("Max retry count exceeded, no failure state defined");
        execution.setNextActivation(null);
      }
    } else {
      execution.setNextActivation(getSettings().getErrorTransitionActivation());
    }
  }

  /**
   * Returns the workflow state method for the given state name.
   * @return The workflow state method, or null if not found.
   */
  public WorkflowStateMethod getMethod(String stateName) {
    return stateMethods.get(stateName);
  }

  /**
   * Returns the workflow state for the given state name.
   * @return The workflos state matching the state name.
   * @throws IllegalStateException when a matching state can not be found.
   */
  public WorkflowState getState(String state) {
    for (WorkflowState s : getStates()) {
      if (state.equals(s.getName())) {
        return s;
      }
    }
    throw new IllegalStateException("No state '" + state + "' in workflow definiton " + getType());
  }

  /**
   * Returns true if the given state is a valid start state, or false otherwise.
   * @throws IllegalStateException if the given state name does not match any state.
   */
  public boolean isStartState(String state) {
    return getState(state).getType() == WorkflowStateType.start;
  }
}
