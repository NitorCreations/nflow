package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to set workflow instance signal value")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class SetSignalRequest extends ModelObject {

  @Schema(description = "New signal value")
  public Integer signal;

  @Schema(description = "Reason for setting the signal")
  public String reason;

}
