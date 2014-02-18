package com.nitorcreations.nflow.rest.v0.msg;

import org.joda.time.DateTime;

public class UpdateWorkflowInstanceRequest {

  public String state;
  public DateTime nextActivationTime;

}
