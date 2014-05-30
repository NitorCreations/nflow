package com.nitorcreations.nflow.rest.v0.msg;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "State change attempt. A new instance for every retry attempt.")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class Action {
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
