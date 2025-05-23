package io.nflow.rest.v1.msg;

import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to set workflow instance signal value")
public class SetSignalRequest extends ModelObject {

  @Schema(description = "New signal value")
  public Integer signal;

  @Schema(description = "Reason for setting the signal")
  public String reason;

}
