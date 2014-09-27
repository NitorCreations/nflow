package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static java.lang.String.format;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;

/**
 * The base class for all workflow definitions.
 */
public abstract class AbstractWorkflowDefinition<S extends WorkflowState> {

  private static final Logger logger = getLogger(AbstractWorkflowDefinition.class);
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
   * If this method is called multiple times for the same origin state,
   * the last failure state will be effective.
   * @param originState The origin state.
   * @param targetState The target state.
   * @param failureState The failure state.
   * @return This.
   */
  protected AbstractWorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    Assert.notNull(failureState, "Failure state can not be null");
    requireStateMethodExists(failureState);
    failureTransitions.put(originState.name(), failureState);
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

  boolean isStateMethodObligatory(S state) {
    return state.getType() != manual && state.getType() != end;
  }

  void requireStateMethodExists(S state) {
    WorkflowStateMethod stateMethod = stateMethods.get(state.name());
    if (stateMethod == null && isStateMethodObligatory(state)) {
      String msg = format(
          "Class '%s' is missing state handling method 'public NextAction %s(StateExecution execution, ... args)'",
          this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
    if (stateMethod != null) {
      WorkflowStateType stateType = state.getType();
      Class<?> returnType = stateMethod.method.getReturnType();
      if (Void.TYPE.equals(returnType) && !stateType.isFinal()) {
        String msg = format(
            "Class '%s' has %s state method '%s' that does not return NextAction",
            this.getClass().getName(), stateType.name(), state.name());
        throw new IllegalArgumentException(msg);
      } else if (NextAction.class.equals(returnType) && stateType.isFinal()) {
        logger.warn("Class '{}' has {} state method '{}' that returns NextAction, should return void. Return value will be ignored. Returning NextAction from final state method will be disallowed in nFlow 2.0.0.",
            this.getClass().getName(), stateType.name(), state.name());
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
      WorkflowState failureState = failureTransitions.get(execution.getCurrentStateName());
      if (failureState != null) {
        execution.setNextState(failureState);
        execution.setNextStateReason("Max retry count exceeded");
        execution.setNextActivation(now());
      } else {
        execution.setNextState(errorState);
        execution.setNextStateReason("Max retry count exceeded, no failure state defined");
        execution.setNextActivation(null);
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
}
