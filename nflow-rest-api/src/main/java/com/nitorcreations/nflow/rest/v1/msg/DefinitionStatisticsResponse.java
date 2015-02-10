package com.nitorcreations.nflow.rest.v1.msg;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Statistics for workflow instance state and status")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class DefinitionStatisticsResponse {

  @ApiModelProperty(value = "The number of all instances of the workflow in given state and status")
  public long allInstances;

  @ApiModelProperty(value = "The number of the instances of the workflow in given state and status that have next activation time in past")
  public long queuedInstances;
}
