package com.nitorcreations.nflow.rest.v1.msg;

import java.util.LinkedHashMap;
import java.util.Map;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Response for workflow definition statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse {

  @ApiModelProperty(value = "Statistics per state", required=true)
  public Map<String, StateStatistics> stateStatistics = new LinkedHashMap<>();

  public static class StateStatistics {
    public AllAndQueued created = new AllAndQueued();
    public AllAndQueued inProgress = new AllAndQueued();
    public All executing = new All();
    public All paused = new All();
    public All stopped = new All();
    public All manual = new All();
    public All finished = new All();

    public static class AllAndQueued extends All {
      public long queuedInstances;
    }

    public static class All {
      public long allInstances;
    }
  }
}
