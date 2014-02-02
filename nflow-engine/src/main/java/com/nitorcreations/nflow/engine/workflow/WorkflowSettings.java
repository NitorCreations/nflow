package com.nitorcreations.nflow.engine.workflow;

public class WorkflowSettings {
  
  public int getErrorBumpedTransitionDelay() {
    return 2 * 3600 * 1000;
  }

  public int getShortTransitionDelay() {
    return 30 * 1000;
  }
  
  public int getImmediateTransitionDelay() {
    return 0;
  }
  
}
