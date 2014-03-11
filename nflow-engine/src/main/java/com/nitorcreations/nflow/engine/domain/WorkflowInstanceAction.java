package com.nitorcreations.nflow.engine.domain;

import org.joda.time.DateTime;

public class WorkflowInstanceAction {

  public final Integer id;
  public final String state;
  public final String stateText;
  public final DateTime nextActivation;
  public final DateTime created;

  public WorkflowInstanceAction(Integer id, String state, String stateText, DateTime nextActivation, DateTime created) {
    this.id = id;
    this.state = state;
    this.stateText = stateText;
    this.nextActivation = nextActivation;
    this.created = created;
  }

}
