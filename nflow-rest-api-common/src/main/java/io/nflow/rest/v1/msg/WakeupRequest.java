package io.nflow.rest.v1.msg;

import static java.util.Collections.emptyList;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Request to wake up a sleeping workflow instance matching the given id if it is in one of the expected states.")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class WakeupRequest extends ModelObject {

  @Schema(description = "List of expected states. Can be empty, meaning any state.")
  public List<String> expectedStates = emptyList();

}
