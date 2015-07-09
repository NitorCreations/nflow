package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Global statistics for workflow instances.")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class QueueStatistics {

  @ApiModelProperty(value = "Number for workflow instances.", required=true)
  public int count;
  @ApiModelProperty("Maximum time (ms) currently queued workflow instances have been in queue.")
  public Long maxAge;
  @ApiModelProperty("Minimum time (ms) currently queued workflow instances have been in queue.")
  public Long minAge;

}
