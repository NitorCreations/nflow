package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "State change attempt. A new instance for every retry attempt.")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class Action {

  @ApiModelProperty(value = "Identifier of the workflow instance action.")
  public int id;
  @ApiModelProperty(value = "Type of state: 'stateExecution', 'stateExecutionFailed', 'externalChange' or 'recovery'.")
  public String type;
  @ApiModelProperty(value = "Name of state")
  public String state;
  @ApiModelProperty(value = "Description of state")
  public String stateText;
  @ApiModelProperty(value = "Number of retries in this state")
  public int retryNo;
  @ApiModelProperty(value = "Start time for execution")
  public DateTime executionStartTime;
  @ApiModelProperty(value = "End time for execution")
  public DateTime executionEndTime;
  @ApiModelProperty(value = "Identifier of the executor that executed this action")
  public int executorId;
  @ApiModelProperty(value = "Updated state variables.", required = false)
  public Map<String, Object> updatedStateVariables;

  public Action() {
  }

  public Action(int id, String type, String state, String stateText, int retryNo, DateTime executionStartTime, DateTime executionEndTime,
      int executorId) {
    this(id, type, state, stateText, retryNo, executionStartTime, executionEndTime, executorId, null);
  }

  public Action(int id, String type, String state, String stateText, int retryNo, DateTime executionStartTime, DateTime executionEndTime,
      int executorId, Map<String, Object> updatedStateVariables) {
    this();
    this.id = id;
    this.type = type;
    this.state = state;
    this.stateText = stateText;
    this.retryNo = retryNo;
    this.executionStartTime = executionStartTime;
    this.executionEndTime = executionEndTime;
    this.executorId = executorId;
    this.updatedStateVariables = updatedStateVariables;
  }
}
