package com.nitorcreations.nflow.rest.v0.msg;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;

public class CreateWorkflowInstanceRequest {

  @NotNull
  @Size(max=30)
  public String type;
  
  @NotNull
  @Size(max=64)
  public String businessKey;

  public JsonNode requestData;
  
  public DateTime activationTime;
    
}
