package com.nitorcreations.nflow.rest.v0.msg;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Response for submit new workflow instance")
public class CreateWorkflowInstanceResponse {

  @ApiModelProperty(value = "Idenfier of the new workflow instance", required=true)
  public int id;

  @ApiModelProperty(value = "Workflow definition identifier (e.g. withdrawLoan", required=true)
  public String type;

  @ApiModelProperty(value = "Main business key or identifier for the started workflow instance (e.g. credit application identifier)", required=false)
  public String businessKey;

}
