package com.nitorcreations.nflow.engine.workflow;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class WorkflowDefinition<S extends WorkflowState> {

  private static final WorkflowSettings defaultSettings = new WorkflowSettings();
  
  private String type;
  private final S initialState;
  private final Map<S,S> allowedTransitions = new LinkedHashMap<>();
  
  protected WorkflowDefinition(String type, S initialState) {
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
    allowedTransitions.put(originState, targetState);
    return this;
  }
  
  public WorkflowSettings getSettings() {
    return defaultSettings;
  }
  
}
