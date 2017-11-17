package io.nflow.rest.v1.msg;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request to start archiving process")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ArchiveRequest extends ModelObject {

  @NotNull
  @ApiModelProperty(value = "Passive workflow instances whose modified time is before this will be archived.", required = true)
  public DateTime olderThan;

  @ApiModelProperty("Number of workflow hierarchies to archive in a single transaction.")
  public int batchSize = 10;
}
