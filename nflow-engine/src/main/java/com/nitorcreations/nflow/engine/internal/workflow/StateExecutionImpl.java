package com.nitorcreations.nflow.engine.internal.workflow;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

public class StateExecutionImpl implements StateExecution {

  private final WorkflowInstance instance;
  private final ObjectStringMapper objectMapper;
  private DateTime nextActivation;
  private String nextState;
  private String nextStateReason;
  private boolean failure = false;
  private boolean saveTrace = true;

  public StateExecutionImpl(WorkflowInstance instance, ObjectStringMapper objectMapper) {
    this.instance = instance;
    this.objectMapper = objectMapper;
  }

  public DateTime getNextActivation() {
    return this.nextActivation;
  }

  public String getNextState() {
    return this.nextState;
  }

  public void setNextState(String state, String reason, DateTime activation) {
    this.nextState = state;
    this.nextStateReason = reason;
    this.nextActivation = activation;
  }

  public String getNextStateReason() {
    return this.nextStateReason;
  }

  public boolean isSaveTrace() {
    return this.saveTrace;
  }

  public String getCurrentStateName() {
    return instance.state;
  }

  @Override
  public String getBusinessKey() {
    return instance.businessKey;
  }

  @Override
  public int getRetries() {
    return instance.retries;
  }

  @Override
  public String getVariable(String name) {
    return getVariable(name, (String) null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getVariable(String name, Class<T> type) {
    return (T) objectMapper.convertToObject(type, name, getVariable(name));
  }

  @Override
  public String getVariable(String name, String defaultValue) {
    if (instance.stateVariables.containsKey(name)) {
      return instance.stateVariables.get(name);
    }
    return defaultValue;
  }

  @Override
  public void setVariable(String name, String value) {
    instance.stateVariables.put(name, value);
  }

  @Override
  public void setVariable(String name, Object value) {
    setVariable(name, objectMapper.convertFromObject(name, value));
  }

  @Override
  public void setNextActivation(DateTime activation) {
    this.nextActivation = activation;
  }

  @Override
  public void setNextState(WorkflowState state) {
    this.nextState = state != null ? state.name() : null;
  }

  @Override
  public void setNextStateReason(String reason) {
    this.nextStateReason = reason;
  }

  @Override
  public void setNextState(WorkflowState state, String reason, DateTime activation) {
    setNextState(state != null ? state.name() : null, reason, activation);
  }

  @Override
  public void setSaveTrace(boolean saveTrace) {
    this.saveTrace = saveTrace;
  }

  @Override
  public boolean isFailure() {
    return failure;
  }

  @Override
  public void setFailure(boolean failure) {
    this.failure = failure;
  }

}
