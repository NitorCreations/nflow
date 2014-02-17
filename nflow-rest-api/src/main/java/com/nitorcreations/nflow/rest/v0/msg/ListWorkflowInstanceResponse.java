package com.nitorcreations.nflow.rest.v0.msg;

import org.joda.time.DateTime;

public class ListWorkflowInstanceResponse {

  public int id;
  public String type;  
  public String businessKey;
  public String state;
  public String stateText;
  public DateTime nextActivation;
  
}
