package com.nitorcreations.nflow.engine.workflow;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class WorkflowDefinition<S extends WorkflowState> {

  private static final WorkflowSettings defaultSettings = new WorkflowSettings();
  
  private String type;
  private final S initialState;
  private final Map<S,S> allowedTransitions = new LinkedHashMap<>();
  
  protected WorkflowDefinition(String type, S initialState) {
    requireStateMethodExists(initialState);
    this.type = type;
    this.initialState = initialState;
  }
  
  public String getType() {
    return type;
  }
  
  public S getInitialState() {
    return initialState;
  }
  
  protected WorkflowDefinition<S> permit(S originState, S targetState) {
    requireStateMethodExists(originState);
    requireStateMethodExists(targetState);
    allowedTransitions.put(originState, targetState);
    return this;
  }
  
  public WorkflowSettings getSettings() {
    return defaultSettings;
  }
  
  private void requireStateMethodExists(S state) {
    try {
      this.getClass().getMethod(state.name(), StateExecution.class);
    } catch (NoSuchMethodException | SecurityException e) {
      String msg = String.format("Class %s is missing state handling method %s(StateExecution execution)", this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
    
  }
}
