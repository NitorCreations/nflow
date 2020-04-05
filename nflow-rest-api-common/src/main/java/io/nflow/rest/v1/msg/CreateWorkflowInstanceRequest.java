package io.nflow.rest.v1.msg;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@Schema(description =
  "Request for submitting a new workflow instance. Note that if externalId is given, " +
  "type and externalId pair must be unique hence enabling retry-safety.")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class CreateWorkflowInstanceRequest extends ModelObject {

  @NotNull
  @Size(max=30)
  @Schema(description = "Workflow definition type", required = true)
  public String type;

  @Size(max=64)
  @Schema(description ="Main business key or identifier for the new workflow instance")
  public String businessKey;

  @Size(max=64)
  @Schema(description ="Start state name (if other than default set in workflow definition)")
  public String startState;

  @Size(max=64)
  @Schema(description ="Unique external identifier within the workflow type. Generated by nflow if not given.")
  public String externalId;

  @Schema(description ="Start time for workflow execution. If null, defaults to now, unless activate is set to false, in which case activationTime is ignored.")
  public DateTime activationTime;

  @Schema(description ="Set to false to force activationTime to null. Default is true.")
  public Boolean activate;

  @Schema(description ="Create the workflow as a child of the given parent workflow.")
  public Long parentWorkflowId;

  @Schema(description ="State variables to be set for the new workflow instance.")
  public Map<String, Object> stateVariables = new HashMap<>();

}
