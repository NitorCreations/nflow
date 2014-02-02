package com.nitorcreations.nflow.rest.v0.msg;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;

public class CreateWorkflowInstanceRequest {

  @NotNull
  @Size(max=30)
  public String type;
  
  @NotNull
  @Size(max=64)
  public String businessKey;

  @Size(max=1024)
  public String requestData;
  
  public DateTime activationTime;
    
}
