package io.nflow.rest.v1.msg;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@Schema(description = "Request for update workflow instance")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class UpdateWorkflowInstanceRequest extends ModelObject {

  @Schema(description = "New state of the workflow instance")
  public String state;

  @Schema(description = "New next activation time for next workflow instance processing")
  public DateTime nextActivationTime;

  @Schema(description = "Description of the action")
  public String actionDescription;

  @Schema(description = "State variables to be added or updated.")
  public Map<String, Object> stateVariables = new HashMap<>();

  @Schema(description = "Business key related to the workflow instance.")
  public String businessKey;
}
