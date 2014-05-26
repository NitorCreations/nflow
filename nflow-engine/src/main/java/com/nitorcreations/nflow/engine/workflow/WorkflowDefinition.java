package com.nitorcreations.nflow.engine.workflow;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;

public abstract class WorkflowDefinition<S extends Enum<S> & WorkflowState> {

  private final String type;
  private String name;
  private String description;
  private final S initialState;
  private final S errorState;
  private final WorkflowSettings settings;
  protected final Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
  protected final Map<String, String> failureTransitions = new LinkedHashMap<>();

  protected WorkflowDefinition(String type, S initialState, S errorState) {
    this(type, initialState, errorState, new WorkflowSettings(null));
  }

  protected WorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    Assert.notNull(initialState, "initialState must not be null");
    Assert.notNull(errorState, "errorState must not be null");
    requireStateMethodExists(initialState);
    this.type = type;
    this.initialState = initialState;
    this.errorState = errorState;
    this.settings = settings;
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

  @SuppressWarnings("unchecked")
  public Set<S> getStates() {
    return EnumSet.allOf(initialState.getClass());
  }

  public Map<String, List<String>> getAllowedTransitions() {
    return new LinkedHashMap<>(allowedTransitions);
  }

  public Map<String, String> getFailureTransitions() {
    return new LinkedHashMap<>(failureTransitions);
  }

  protected WorkflowDefinition<S> permit(S originState, S targetState) {
    return permit(originState, targetState, null);
  }

  protected WorkflowDefinition<S> permit(S originState, S targetState, S failureState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitionsFor(originState).add(targetState.name());
    if (failureState != null) {
      requireStateMethodExists(failureState);
      failureTransitions.put(originState.name(), failureState.name());
    }
    return this;
  }

  private List<String> allowedTransitionsFor(S state) {
    if (!allowedTransitions.containsKey(state.name())) {
      allowedTransitions.put(state.name(), new ArrayList<String>());
    }
    return allowedTransitions.get(state.name());
  }

  public WorkflowSettings getSettings() {
    return settings;
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
      } else {
        execution.setNextState(errorState, "Max retry count exceeded, no failure state defined", null);
        execution.setFailure(false);
      }
    } else {
      execution.setNextActivation(getSettings().getErrorTransitionActivation());
    }
  }

}
