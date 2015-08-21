package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Archiving result response")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ArchiveResponse {

  @ApiModelProperty("Total number of archived workflows")
  public int archivedWorkflows;

}
