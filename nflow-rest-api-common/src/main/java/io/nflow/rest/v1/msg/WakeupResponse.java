package io.nflow.rest.v1.msg;

import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response to wake up request.")
public class WakeupResponse extends ModelObject {

  @Schema(description = "True if the instance was woken up, false otherwise.")
  public boolean wakeupSuccess;

}
