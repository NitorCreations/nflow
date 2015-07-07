package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Response for statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class StatisticsResponse {

  @ApiModelProperty(value = "Statistics for queued workflows. Workflows waiting for free executors.", required=true)
  public QueueStatistics queueStatistics = new QueueStatistics();

  @ApiModelProperty(value = "Statistics for workflows in execution. Workflows currently processed by an executor.", required=true)
  public QueueStatistics executionStatistics = new QueueStatistics();

}
