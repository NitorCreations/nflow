package io.nflow.rest.v1.msg;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for workflow definition statistics")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse extends ModelObject {

  @Schema(description = "Statistics per state", required = true)
  public Map<String, StateStatistics> stateStatistics = new LinkedHashMap<>();

  @Schema(description = "Statistics for a state")
  public static class StateStatistics extends ModelObject {
    @Schema(description = "Instances created", required = true)
    public AllAndQueued created = new AllAndQueued();
    @Schema(description = "Instances in progress", required = true)
    public AllAndQueued inProgress = new AllAndQueued();
    @Schema(description = "Instances executing", required = true)
    public All executing = new All();
    @Schema(description = "Instances in manual state", required = true)
    public All manual = new All();
    @Schema(description = "Instances finished", required = true)
    public All finished = new All();

    @Schema(description = "All and queued instances")
    public static class AllAndQueued extends StateStatistics.All {
      @Schema(description = "Statistics per state", required = true)
      public long queuedInstances;
    }

    @Schema(description = "All instances")
    public static class All extends ModelObject {
      @Schema(description = "Statistics per state", required = true)
      public long allInstances;
    }
  }
}
