package com.nitorcreations.nflow.rest.v0.msg;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Request for submit new workflow instance")
public class CreateWorkflowInstanceRequest {

  @NotNull
  @Size(max=30)
  @ApiModelProperty(value = "Workflow definition identifier (e.g. withdrawLoan)", required=true)
  public String type;

  @NotNull
  @Size(max=64)
  @ApiModelProperty(value = "Main business key or identifier for the started workflow instance (e.g. credit application identifier)", required=false)
  public String businessKey;

  @ApiModelProperty(value = "JSON document that contains business information", required=false)
  public JsonNode requestData;

  @ApiModelProperty(value = "Start time for workflow execution, if missing 'now'", required=false)
  public DateTime activationTime;

}
