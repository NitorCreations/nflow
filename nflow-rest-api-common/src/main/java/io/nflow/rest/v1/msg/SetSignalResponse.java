package io.nflow.rest.v1.msg;

import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response to set signal request.")
public class SetSignalResponse extends ModelObject {

  @Schema(description = "True if the signal was set, false otherwise.")
  public boolean setSignalSuccess;

}
