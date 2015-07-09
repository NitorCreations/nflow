package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Response for workflow definition statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse {

  @ApiModelProperty(value = "Statistics per state", required = true)
  public Map<String, StateStatistics> stateStatistics = new LinkedHashMap<>();

  @ApiModel(description = "Statistics for a state")
  public static class StateStatistics {
    @ApiModelProperty(value = "Instances created", required = true)
    public AllAndQueued created = new AllAndQueued();
    @ApiModelProperty(value = "Instances in progress", required = true)
    public AllAndQueued inProgress = new AllAndQueued();
    @ApiModelProperty(value = "Instances executing", required = true)
    public All executing = new All();
    @ApiModelProperty(value = "Instances in manual state", required = true)
    public All manual = new All();
    @ApiModelProperty(value = "Instances finished", required = true)
    public All finished = new All();

    @ApiModel(description = "All and queued instances")
    public static class AllAndQueued extends StateStatistics.All {
      @ApiModelProperty(value = "Statistics per state", required = true)
      public long queuedInstances;
    }

    @ApiModel(description = "All instances")
    public static class All {
      @ApiModelProperty(value = "Statistics per state", required = true)
      public long allInstances;
    }
  }
}
