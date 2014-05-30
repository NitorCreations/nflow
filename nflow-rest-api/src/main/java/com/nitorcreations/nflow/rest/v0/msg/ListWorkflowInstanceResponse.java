package com.nitorcreations.nflow.rest.v0.msg;

import java.util.List;

import org.joda.time.DateTime;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@ApiModel(value = "Basic information of workflow instance")
@SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class ListWorkflowInstanceResponse {

  @ApiModelProperty(value = "Idenfier of the new workflow instance", required=true)
  public int id;

  @ApiModelProperty(value = "Workflow definition identifier", required=true)
  public String type;

  @ApiModelProperty(value = "Main business key or identifier for the started workflow instance", required=false)
  public String businessKey;

  @ApiModelProperty(value = "State of the workflow instance", required=true)
  public String state;

  @ApiModelProperty(value = "Text of describing the reason for state (free text)", required=false)
  public String stateText;

  @ApiModelProperty(value = "Next activation time for workflow instance processing", required=false)
  public DateTime nextActivation;

  @ApiModelProperty(value = "Next activation time for workflow instance processing", required=false)
  public List<Action> actions;

  @ApiModel(value = "State change attempt. A new instance for every retry attempt.")
  public static class Action {
    @ApiModelProperty(value = "Name of state")
    public String state;
    @ApiModelProperty(value = "Description of state")
    public String stateText;
    @ApiModelProperty(value = "Number of retries in this state")
    public int retryNo;
    @ApiModelProperty(value = "Start time for execution (msec)")
    public long executionStart;
    @ApiModelProperty(value = "End time for execution (msec)")
    public long executionEnd;

    public Action() {}

    public Action(String state, String stateText, int retryNo, long executionStart, long executionEnd) {
      this.state = state;
      this.stateText = stateText;
      this.retryNo = retryNo;
      this.executionStart = executionStart;
      this.executionEnd = executionEnd;
    }
  }

}
