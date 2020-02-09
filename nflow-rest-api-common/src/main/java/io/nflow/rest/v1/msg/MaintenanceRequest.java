package io.nflow.rest.v1.msg;

import org.joda.time.Duration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request to start archiving process")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceRequest extends ModelObject {

  @ApiModelProperty("Archived workflow instances whose modified time is older than given duration will be deleted.")
  public Duration deleteArchivedWorkflowsOlderThan;

  @ApiModelProperty("Passive workflow instances whose modified time is older than given duration will be archived.")
  public Duration archiveWorkflowsOlderThan;

  @ApiModelProperty("Passive workflow instances whose modified time is older than given duration will be deleted.")
  public Duration deleteWorkflowsOlderThan;

  @ApiModelProperty("Number of workflow hierarchies to archive in a single transaction.")
  public int batchSize = 10;
}
