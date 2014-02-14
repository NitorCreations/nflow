package com.nitorcreations.nflow.engine.workflow;

import org.joda.time.DateTime;

public interface StateExecution {
    
  public String getBusinessKey();
  public String getRequestData();
  public int getRetries();
  public boolean isFailure();
  
  public String getVariable(String name);
  public String getVariable(String name, String defaultValue);
  public void setVariable(String name, String value);
  
  public void setNextActivation(DateTime time);
  public void setNextState(WorkflowState state);
  public void setNextStateReason(String stateText);
  public void setNextState(WorkflowState state, String stateText, DateTime time);
  public void setFailure(boolean failure);
  public void setSaveTrace(boolean saveTrace);
  
}
