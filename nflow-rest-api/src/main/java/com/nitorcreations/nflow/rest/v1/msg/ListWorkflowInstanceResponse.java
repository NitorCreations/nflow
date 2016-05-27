package com.nitorcreations.nflow.rest.v1.msg;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Basic information of workflow instance")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class ListWorkflowInstanceResponse {

  @ApiModelProperty(value = "Identifier of the workflow instance", required = true)
  public int id;

  @ApiModelProperty(value = "Workflow instance status (created, executing, inProgress, finished, manual)", required = true)
  public String status;

  @ApiModelProperty(value = "Workflow definition type", required = true)
  public String type;

  @ApiModelProperty("Parent workflow instance id for child workflows")
  public Integer parentWorkflowId;

  @ApiModelProperty("Parent workflow instance action id for child workflows (action that created the child workflow)")
  public Integer parentActionId;

  @ApiModelProperty("Main business key or identifier for the workflow instance")
  public String businessKey;

  @ApiModelProperty(value = "Unique external identifier within a workflow type. Generated by nflow if not given.", required = true)
  public String externalId;

  @ApiModelProperty(value = "State of the workflow instance", required=true)
  public String state;

  @ApiModelProperty("Text of describing the reason for state (free text)")
  public String stateText;

  @ApiModelProperty("Time when the workflow instance is processed again")
  public DateTime nextActivation;

  @ApiModelProperty("State variables for current state")
  public Map<String, Object> stateVariables;

  @ApiModelProperty(value = "Number of times the current state has been retried", required = true)
  public int retries;

  @ApiModelProperty("Action history of the workflow instance")
  public List<Action> actions;

  @ApiModelProperty(value = "Workflow instance creation timestamp", required=true)
  public DateTime created;

  @ApiModelProperty(value = "Workflow instance latest modification timestamp", required=true)
  public DateTime modified;

  @ApiModelProperty("Time when workflow processing started (=start time of the first action)")
  public DateTime started;

  @ApiModelProperty("Child workflow instance IDs created by this instance, grouped by instance action ID")
  public Map<Integer, List<Integer>> childWorkflows;
}