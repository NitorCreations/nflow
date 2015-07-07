package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Basic information of workflow executor")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class ListWorkflowExecutorResponse {

  @ApiModelProperty(value = "Identifier of the workflow executor", required=true)
  public int id;

  @ApiModelProperty(value = "Host where the executor is running", required=true)
  public String host;

  @ApiModelProperty(value = "Executor process identifier assigned by the operating system", required=true)
  public int pid;

  @ApiModelProperty(value = "Executor group the executor belongs to", required=true)
  public String executorGroup;

  @ApiModelProperty(value = "Time when the executor was started", required=true)
  public DateTime started;

  @ApiModelProperty(value = "Last time the executor updated it's heart beat to the database", required=true)
  public DateTime active;

  @ApiModelProperty(value = "Time after which the executor is considered as crashed", required=true)
  public DateTime expires;
}
