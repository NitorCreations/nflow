package io.nflow.rest.v1.msg;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response for workflow definition statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse extends ModelObject {

  @ApiModelProperty(value = "Statistics per state", required = true)
  // TODO: Swagger fails to scan StateStatistics-class because it is referenced by a Map. There's multiple Swagger issues open
  // related to this. Follow issue: https://github.com/swagger-api/swagger-ui/issues/1248
  public Map<String, StateStatistics> stateStatistics = new LinkedHashMap<>();

  @ApiModel(description = "Statistics for a state")
  public static class StateStatistics extends ModelObject {
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
    public static class All extends ModelObject {
      @ApiModelProperty(value = "Statistics per state", required = true)
      public long allInstances;
    }
  }
}
