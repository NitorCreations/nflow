package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Maintenance result response")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceResponse extends ModelObject {

  @Schema(description = "Total number of deleted archived workflows")
  public int deletedArchivedWorkflows;

  @Schema(description = "Total number of archived workflows")
  public int archivedWorkflows;

  @Schema(description = "Total number of deleted workflows")
  public int deletedWorkflows;

}
