package io.nflow.rest.v1.msg;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Response for workflow definition statistics")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class WorkflowDefinitionStatisticsResponse extends ModelObject {

  @Schema(description = "Statistics per state", requiredMode = REQUIRED)
  public Map<String, StateStatistics> stateStatistics = new LinkedHashMap<>();

  @Schema(description = "Statistics for a state")
  public static class StateStatistics extends ModelObject {
    @Schema(description = "Instances created", requiredMode = REQUIRED)
    public AllAndQueued created = new AllAndQueued();
    @Schema(description = "Instances in progress", requiredMode = REQUIRED)
    public AllAndQueued inProgress = new AllAndQueued();
    @Schema(description = "Instances executing", requiredMode = REQUIRED)
    public All executing = new All();
    @Schema(description = "Instances in manual state", requiredMode = REQUIRED)
    public All manual = new All();
    @Schema(description = "Instances finished", requiredMode = REQUIRED)
    public All finished = new All();

    @Schema(description = "All and queued instances")
    public static class AllAndQueued extends StateStatistics.All {
      @Schema(description = "Statistics per state", requiredMode = REQUIRED)
      public long queuedInstances;
    }

    @Schema(description = "All instances")
    public static class All extends ModelObject {
      @Schema(description = "Statistics per state", requiredMode = REQUIRED)
      public long allInstances;
    }
  }
}
