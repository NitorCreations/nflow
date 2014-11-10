package com.nitorcreations.nflow.rest.v1.msg;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Global statistics for workflow instances.")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class QueueStatistics {

  @ApiModelProperty(value = "Number for workflow instances", required=true)
  public int count;
  @ApiModelProperty(value = "Maximum time currently queued workflow instances have been in queue. In msec.", required=false)
  public Long maxAge;
  @ApiModelProperty(value = "Min time currently queued workflow instances have been in queue. In msec.", required=false)
  public Long minAge;

}
