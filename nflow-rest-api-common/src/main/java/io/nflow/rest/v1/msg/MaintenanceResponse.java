package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Maintenance result response")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class MaintenanceResponse extends ModelObject {

  @ApiModelProperty("Total number of deleted archived workflows")
  public int deletedArchivedWorkflows;

  @ApiModelProperty("Total number of archived workflows")
  public int archivedWorkflows;

  @ApiModelProperty("Total number of deleted workflows")
  public int deletedWorkflows;

}
