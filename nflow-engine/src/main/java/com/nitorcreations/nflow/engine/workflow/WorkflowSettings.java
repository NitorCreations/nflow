package com.nitorcreations.nflow.engine.workflow;

import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;

public class WorkflowSettings {

  public DateTime getErrorTransitionActivation() {
    return now().plusMillis(getErrorTransitionDelay());
  }

  public int getErrorTransitionDelay() {
    return 2 * 3600 * 1000;
  }

  public DateTime getShortTransitionActivation() {
    return now().plusMillis(getShortTransitionDelay());
  }

  public int getShortTransitionDelay() {
    return 30 * 1000;
  }

  public int getImmediateTransitionDelay() {
    return 0;
  }

  public int getMaxRetries() {
    return 3;
  }

}
