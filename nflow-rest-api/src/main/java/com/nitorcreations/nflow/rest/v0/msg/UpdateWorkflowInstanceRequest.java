package com.nitorcreations.nflow.rest.v0.msg;

import org.joda.time.DateTime;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Request for update workflow instance")
public class UpdateWorkflowInstanceRequest {

  @ApiModelProperty(value = "New state of the workflow instance", required=false)
  public String state;

  @ApiModelProperty(value = "New next activation time for next workflow instance processing", required=false)
  public DateTime nextActivationTime;

}
