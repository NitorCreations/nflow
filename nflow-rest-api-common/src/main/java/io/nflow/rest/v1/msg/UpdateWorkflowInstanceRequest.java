package io.nflow.rest.v1.msg;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for update workflow instance")
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
