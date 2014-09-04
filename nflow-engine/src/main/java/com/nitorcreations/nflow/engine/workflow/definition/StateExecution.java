package com.nitorcreations.nflow.engine.workflow.definition;


public interface StateExecution {

  String getBusinessKey();

  int getRetries();

  String getVariable(String name);
  <T> T getVariable(String name, Class<T> type);
  String getVariable(String name, String defaultValue);
  void setVariable(String name, String value);
  void setVariable(String name, Object value);
}
