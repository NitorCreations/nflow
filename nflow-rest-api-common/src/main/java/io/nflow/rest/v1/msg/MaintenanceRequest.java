package io.nflow.rest.v1.msg;

import static java.util.Collections.emptySet;

import java.util.Set;

import org.joda.time.ReadablePeriod;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to start maintenance process")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceRequest extends ModelObject {

  @Schema(description = "Delete archived workflows")
  public MaintenanceRequestItem deleteArchivedWorkflows;

  @Schema(description = "Archive passive workflows")
  public MaintenanceRequestItem archiveWorkflows;

  @Schema(description = "Delete passive workflows")
  public MaintenanceRequestItem deleteWorkflows;

  public static class MaintenanceRequestItem extends ModelObject {
    @Schema(
        description = "Workflow instances whose modified time is older than given period will be processed. Supports ISO-8601 format.",
        type = "string", format = "duration", example = "PT15D", required = true)
    public ReadablePeriod olderThanPeriod;

    @Schema(description = "Number of workflows to process in a single transaction.", example = "1000", defaultValue = "1000", minimum = "1")
    public int batchSize = 1000;

    @Schema(description = "Workflow types to process. If no types are defined, process all types.")
    public Set<String> workflowTypes = emptySet();
  }

}
