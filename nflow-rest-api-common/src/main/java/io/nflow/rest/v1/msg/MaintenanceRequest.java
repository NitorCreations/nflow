package io.nflow.rest.v1.msg;

import static java.util.Collections.emptySet;

import java.util.Set;

import org.joda.time.ReadablePeriod;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request to start maintenance process")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceRequest extends ModelObject {

  @ApiModelProperty("Delete archived workflows")
  public MaintenanceRequestItem deleteArchivedWorkflows;

  @ApiModelProperty("Archive passive workflows")
  public MaintenanceRequestItem archiveWorkflows;

  @ApiModelProperty("Delete passive workflows")
  public MaintenanceRequestItem deleteWorkflows;

  public static class MaintenanceRequestItem extends ModelObject {
    @ApiModelProperty(value = "Workflow instances whose modified time is older than given period will be processed. Supports ISO-8601 format.", //
        dataType = "String", example = "PT15D", required = true)
    public ReadablePeriod olderThanPeriod;

    @ApiModelProperty(value = "Number of workflows to process in a single transaction.", example = "1000")
    public int batchSize = 1000;

    @ApiModelProperty("Workflow types to process. If no types are defined, process all types.")
    public Set<String> workflowTypes = emptySet();
  }

}
