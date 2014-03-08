package com.nitorcreations.nflow.engine.workflow;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;

public abstract class WorkflowDefinition<S extends WorkflowState> {

  private static final WorkflowSettings defaultSettings = new WorkflowSettings();

  private final String type;
  private String name;
  private String description;
  private final S initialState;
  private final S errorState;
  protected final Map<String, String> allowedTransitions = new LinkedHashMap<>();
  protected final Map<String, String> failureTransitions = new LinkedHashMap<>();

  protected WorkflowDefinition(String type, S initialState, S errorState) {
    requireStateMethodExists(initialState);
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public S getInitialState() {
    return initialState;
  }

  public S getErrorState() {
    return errorState;
  }

  public Set<? extends WorkflowState> getStates() {
    Class<?> clazz = initialState.getClass();
    Class<? extends WorkflowState> stateClass;
    try {
      stateClass = clazz.asSubclass(WorkflowState.class);
    } catch (ClassCastException cce) {
      throw new IllegalStateException("Specified type doesn't implement WorkflowState", cce);
    }
    @SuppressWarnings("unchecked")
    S[] enumConstants = (S[]) stateClass.getEnumConstants();
    if (enumConstants == null) {
        throw new IllegalStateException("Specified type is not an enum.");
    }
    return new HashSet<S>(asList(enumConstants));
  }

  public Map<String, String> getAllowedTransitions() {
    return new HashMap<>(allowedTransitions);
  }

  public Map<String, String> getFailureTransitions() {
    return new HashMap<>(failureTransitions);
  }

  protected WorkflowDefinition<S> permit(S originState, S targetState) {
    return permit(originState, targetState, null);
  }

  protected WorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitions.put(originState.name(), targetState.name());
    if (failureState != null) {
      requireStateMethodExists(failureState);
      failureTransitions.put(originState.name(), failureState.name());
    }
    return this;
  }

  public WorkflowSettings getSettings() {
    return defaultSettings;
  }

  private void requireStateMethodExists(S state) {
    if(ReflectionUtils.findMethod(this.getClass(), state.name(), StateExecution.class) == null) {
      String msg = String.format("Class %s is missing state handling method %s(StateExecution execution)", this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
  }

  public void handleRetry(StateExecutionImpl execution) {
    if (execution.getRetries() >= getSettings().getMaxRetries()) {
      String failureState = failureTransitions.get(execution.getCurrentStateName());
      if (failureState != null) {
        execution.setNextState(failureState, "Max retry count exceeded", getSettings().getErrorTransitionActivation());
        execution.setFailure(false);
      } else if (errorState != null) {
        execution.setNextState(errorState, "Max retry count exceeded, no failure state defined", null);
        execution.setFailure(false);
      } else {
        execution.setNextStateReason("Max retry count exceeded, no error state defined");
        execution.setNextActivation(null);
      }
    } else {
      execution.setNextActivation(getSettings().getErrorTransitionActivation());
    }
  }

}
