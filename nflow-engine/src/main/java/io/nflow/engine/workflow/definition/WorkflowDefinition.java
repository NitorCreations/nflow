package io.nflow.engine.workflow.definition;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.util.Assert;

import io.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import io.nflow.engine.internal.workflow.WorkflowStateMethod;
import io.nflow.engine.model.ModelObject;
import io.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * The base class for all workflow definitions. Extending workflow definition classes should register all their states using at
 * least one of the following ways:
 * <ul>
 * <li>Using them as initialState or errorState parameter when calling the super constructor</li>
 * <li>Using them as one of the parameters when registering allowed state transfers using <code>permit()</code> method</li>
 * <li>Defining them as public static fields in the workflow definition class</li>
 * <li>Registering them using <code>registerState()</code> method</li>
 * <li>Passing them to super constructor in <code>states</code> parameter</li>
 * </ul>
 */
public abstract class WorkflowDefinition extends ModelObject {

  private final String type;
  private String name;
  private String description;
  private final WorkflowState initialState;
  private final WorkflowState errorState;
  private final WorkflowSettings settings;
  protected final Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
  protected final Map<String, WorkflowState> failureTransitions = new LinkedHashMap<>();
  private Map<String, WorkflowStateMethod> stateMethods;
  private final Set<WorkflowState> states = new HashSet<>();
  private boolean verifyStateMethodValidity;

  /**
   * Create a workflow definition with default settings and automatically scanned state methods.
   *
   * @param type
   *          The unique identifier of this workflow definition.
   * @param initialState
   *          The default start state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param errorState
   *          The default error state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   */
  protected WorkflowDefinition(String type, WorkflowState initialState, WorkflowState errorState) {
    this(type, initialState, errorState, new WorkflowSettings.Builder().build());
  }

  /**
   * Create a workflow definition with given settings and automatically scanned state methods.
   *
   * @param type
   *          The unique identifier of this workflow definition.
   * @param initialState
   *          The default start state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param errorState
   *          The default error state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param settings
   *          The configuration for the workflow instances of this workflow type.
   */
  protected WorkflowDefinition(String type, WorkflowState initialState, WorkflowState errorState, WorkflowSettings settings) {
    this(type, initialState, errorState, settings, null);
  }

  /**
   * Create a workflow definition with given settings and state methods.
   *
   * @param type
   *          The unique identifier of this workflow definition.
   * @param initialState
   *          The default start state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param errorState
   *          The default error state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param settings
   *          The configuration for the workflow instances of this workflow type.
   * @param stateMethods
   *          The state methods to be used for the states of this workflow type. If null, the methods will be scanned.
   */
  protected WorkflowDefinition(String type, WorkflowState initialState, WorkflowState errorState, WorkflowSettings settings,
      Map<String, WorkflowStateMethod> stateMethods) {
    this(type, initialState, errorState, settings, stateMethods, null, true);
  }

  /**
   * Create a workflow definition with given settings, state methods and states.
   *
   * @param type
   *          The unique identifier of this workflow definition.
   * @param initialState
   *          The default start state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param errorState
   *          The default error state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param settings
   *          The configuration for the workflow instances of this workflow type.
   * @param stateMethods
   *          The state methods to be used for the states of this workflow type. If null, the methods will be scanned.
   * @param states
   *          The states to be registered for the workflow. If null, the states will be scanned.
   */
  protected WorkflowDefinition(String type, WorkflowState initialState, WorkflowState errorState, WorkflowSettings settings,
                               Map<String, WorkflowStateMethod> stateMethods, Collection<WorkflowState> states) {
    this(type, initialState, errorState, settings, stateMethods, states, true);
  }

  /**
   * Create a workflow definition with given settings, state methods and states.
   *
   * @param type
   *          The unique identifier of this workflow definition.
   * @param initialState
   *          The default start state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param errorState
   *          The default error state of the workflow. The state is automatically registered as one of the allowed states in this
   *          workflow.
   * @param settings
   *          The configuration for the workflow instances of this workflow type.
   * @param stateMethods
   *          The state methods to be used for the states of this workflow type. If null, the methods will be scanned.
   * @param states
   *          The states to be registered for the workflow. If null, the states will be scanned.
   * @param verifyStateMethodValidity
   *          True to search and verify the implementation of registered state methods to ensure they comply with expectations.
   */
  protected WorkflowDefinition(String type, WorkflowState initialState, WorkflowState errorState, WorkflowSettings settings,
      Map<String, WorkflowStateMethod> stateMethods, Collection<WorkflowState> states, boolean verifyStateMethodValidity) {
    Assert.notNull(initialState, "initialState must not be null");
    Assert.isTrue(initialState.getType() == WorkflowStateType.start, "initialState must be a start state");
    Assert.notNull(errorState, "errorState must not be null");
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
    this.settings = settings;
    this.verifyStateMethodValidity = verifyStateMethodValidity;
    WorkflowDefinitionScanner scanner = new WorkflowDefinitionScanner();
    if (stateMethods == null) {
      Assert.isTrue(verifyStateMethodValidity, "Must enable validation if state methods are null and thus scanned");
      this.stateMethods = scanner.getStateMethods(getClass());
    } else {
      if (!verifyStateMethodValidity) {
        Assert.isTrue(stateMethods.isEmpty(), "Must enable validation if state methods provided");
      }
      this.stateMethods = stateMethods;
    }
    registerState(initialState);
    registerState(errorState);
    if (states == null) {
      scanner.getPublicStaticWorkflowStates(getClass()).forEach(this::registerState);
    } else {
      states.forEach(this::registerState);
    }
  }

  /**
   * Return the name of the workflow.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the workflow.
   *
   * @param name
   *          The name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the description of the workflow.
   *
   * @return The description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description of the workflow.
   *
   * @param description
   *          The description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Return the type of the workflow.
   *
   * @return The type.
   */
  public String getType() {
    return type;
  }

  /**
   * Return the initial state of the workflow.
   *
   * @return Workflow state.
   */
  public WorkflowState getInitialState() {
    return initialState;
  }

  /**
   * Return the generic error state of the workflow.
   *
   * @return Workflow state.
   */
  public WorkflowState getErrorState() {
    return errorState;
  }

  /**
   * Return all possible states of the workflow.
   *
   * @return Set of workflow states.
   */
  public Set<WorkflowState> getStates() {
    return new HashSet<>(states);
  }

  /**
   * Register a state as one of the allowed states in this workflow.
   *
   * @param state
   *          The state to be registered.
   */
  public final void registerState(WorkflowState state) {
    requireStateMethodExists(state);
    states.add(state);
  }

  /**
   * Return allowed transitions between the states of the workflow.
   *
   * @return Map from origin states to a list of target states.
   */
  public Map<String, List<String>> getAllowedTransitions() {
    return new LinkedHashMap<>(allowedTransitions);
  }

  /**
   * Return transitions from states to failure states.
   *
   * @return Map from origin state to failure state.
   */
  public Map<String, WorkflowState> getFailureTransitions() {
    return new LinkedHashMap<>(failureTransitions);
  }

  /**
   * Register a state transition to the allowed transitions.
   *
   * @param originState
   *          The origin state. The state is automatically registered as one of the allowed states in this workflow.
   * @param targetState
   *          The target state. The state is automatically registered as one of the allowed states in this workflow.
   * @return This.
   */
  protected WorkflowDefinition permit(WorkflowState originState, WorkflowState targetState) {
    registerState(originState);
    registerState(targetState);
    allowedTransitionsFor(originState).add(targetState.name());
    return this;
  }

  /**
   * Register a state and failure state transitions to the allowed transitions.
   *
   * @param originState
   *          The origin state. The state is automatically registered as one of the allowed states in this workflow.
   * @param targetState
   *          The target state. The state is automatically registered as one of the allowed states in this workflow.
   * @param failureState
   *          The failure state. The state is automatically registered as one of the allowed states in this workflow.
   * @return This.
   */
  protected WorkflowDefinition permit(WorkflowState originState, WorkflowState targetState, WorkflowState failureState) {
    Assert.notNull(failureState, "Failure state can not be null");
    registerState(failureState);
    WorkflowState existingFailure = failureTransitions.put(originState.name(), failureState);
    if (existingFailure != null) {
      Assert.isTrue(existingFailure.equals(failureState),
          "Different failureState '" + existingFailure.name() + "' already defined for originState '" + originState.name() + "'");
    }
    return permit(originState, targetState);
  }

  private List<String> allowedTransitionsFor(WorkflowState state) {
    String stateName = state.name();
    List<String> transitions = allowedTransitions.get(stateName);
    if (transitions == null) {
      transitions = new ArrayList<>();
      allowedTransitions.put(stateName, transitions);
    }
    return transitions;
  }

  /**
   * Return the workflow settings.
   *
   * @return Workflow settings.
   */
  public WorkflowSettings getSettings() {
    return settings;
  }

  final void requireStateMethodExists(WorkflowState state) {
    if (!verifyStateMethodValidity) {
      return;
    }
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
        String msg = format("Class '%s' has a non-final state method '%s' that does not return NextAction",
            this.getClass().getName(), state.name());
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
   * Returns the workflow state method for the given state name.
   *
   * @param stateName
   *          The name of the workflow state.
   * @return The workflow state method, or null if not found.
   */
  public WorkflowStateMethod getMethod(String stateName) {
    return stateMethods.get(stateName);
  }

  /**
   * Returns the workflow state method for the given state.
   *
   * @param state
   *          The workflow state.
   * @return The workflow state method, or null if not found.
   */
  public WorkflowStateMethod getMethod(WorkflowState state) {
    return stateMethods.get(state.name());
  }

  /**
   * Returns the workflow state for the given state name.
   *
   * @param state
   *          The name of the workflow state.
   * @return The workflos state matching the state name.
   * @throws IllegalArgumentException
   *           when a matching state can not be found.
   */
  public WorkflowState getState(String state) {
    return getStates().stream().filter(s -> Objects.equals(s.name(), state)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No state '" + state + "' in workflow definiton " + getType()));
  }

  /**
   * Check if the given state is a valid start state.
   *
   * @param state
   *          The name of the workflow state.
   * @return True if the given state is a valid start date, false otherwise.
   * @throws IllegalArgumentException
   *           if the given state name does not match any state.
   */
  public boolean isStartState(String state) {
    return getState(state).getType() == WorkflowStateType.start;
  }

  /**
   * Return true if the given nextAction is permitted for given instance.
   *
   * @param instance
   *          The workflow instance for which the action is checked.
   * @param nextAction
   *          The action to be checked.
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

  /**
   * Return signals supported by this workflow. Default implementation returns empty map, override this in your workflow
   * definition that supports signals.
   *
   * @return Signals and their descriptions.
   */
  public Map<Integer, String> getSupportedSignals() {
    return emptyMap();
  }
}
