package io.nflow.rest.v1.msg;

import org.joda.time.ReadablePeriod;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request to start maintenance process")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceRequest extends ModelObject {

  @ApiModelProperty("Archived workflow instances whose modified time is older than given period will be deleted.")
  public ReadablePeriod deleteArchivedWorkflowsOlderThan;

  @ApiModelProperty("Passive workflow instances whose modified time is older than given period will be archived.")
  public ReadablePeriod archiveWorkflowsOlderThan;

  @ApiModelProperty("Passive workflow instances whose modified time is older than given period will be deleted.")
  public ReadablePeriod deleteWorkflowsOlderThan;

  @ApiModelProperty("Number of workflows to process in a single transaction.")
  public int batchSize = 1000;
}
