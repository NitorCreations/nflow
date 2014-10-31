package com.nitorcreations.nflow.rest.v1.msg;

import java.util.HashMap;
import java.util.Map;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Response for workflow definition statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse {

  @ApiModelProperty(value = "Statistics per state", required=true)
  public Map<String, Map<String, Long>> stateStatistics = new HashMap<>();
}
