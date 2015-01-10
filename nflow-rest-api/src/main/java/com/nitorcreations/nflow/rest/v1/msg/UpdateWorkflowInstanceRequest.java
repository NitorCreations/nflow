package com.nitorcreations.nflow.rest.v1.msg;

import org.joda.time.DateTime;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Request for update workflow instance")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class UpdateWorkflowInstanceRequest {

  @ApiModelProperty(value = "New state of the workflow instance", required=false)
  public String state;

  @ApiModelProperty(value = "New next activation time for next workflow instance processing", required=false)
  public DateTime nextActivationTime;

  @ApiModelProperty(value = "Description of the action", required = false)
  public String actionDescription;
}
