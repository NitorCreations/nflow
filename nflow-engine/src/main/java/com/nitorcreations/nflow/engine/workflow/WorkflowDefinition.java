package com.nitorcreations.nflow.engine.workflow;

import static org.joda.time.DateTime.now;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.util.ReflectionUtils;

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
    if(ReflectionUtils.findMethod(this.getClass(), state.name(), StateExecution.class) == null) {
      String msg = String.format("Class %s is missing state handling method %s(StateExecution execution)", this.getClass().getName(), state.name());
      throw new IllegalArgumentException(msg);
    }
  }
  
  protected DateTime errorDelay() {
    return now().plusMillis(getSettings().getErrorTransitionDelay());
  }
  
  protected void handleRetry(S retryState, S failureState, String msg, StateExecution execution) {
    if (execution.getRetries() > getSettings().getMaxRetries()) {
      execution.setNextState(failureState, msg, now());
    } else {
      execution.setFailure(true);
      execution.setNextState(retryState, msg, errorDelay());
    }
  }
  
}
