package com.nitorcreations.nflow.engine.workflow;

import org.joda.time.DateTime;

public interface StateExecution {

  String getBusinessKey();
  String getRequestData();
  int getRetries();
  boolean isFailure();

  String getVariable(String name);
  String getVariable(String name, String defaultValue);
  void setVariable(String name, String value);

  void setNextActivation(DateTime time);
  void setNextState(WorkflowState state);
  void setNextStateReason(String stateText);
  void setNextState(WorkflowState state, String stateText, DateTime time);
  void setFailure(boolean failure);
  void setSaveTrace(boolean saveTrace);

}
