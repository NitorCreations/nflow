package io.nflow.rest.v1.msg;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.nitorcreations.nflow.engine.model.ModelObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description =
  "Request for submitting a new workflow instance. Note that if externalId is given, " +
  "type and externalId pair must be unique hence enabling retry-safety.")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class CreateWorkflowInstanceRequest extends ModelObject {

  @NotNull
  @Size(max=30)
  @ApiModelProperty(value = "Workflow definition type", required = true)
  public String type;

  @Size(max=64)
  @ApiModelProperty("Main business key or identifier for the new workflow instance")
  public String businessKey;

  @Size(max=64)
  @ApiModelProperty("Start state name (if other than default set in workflow definition)")
  public String startState;

  @Size(max=64)
  @ApiModelProperty("Unique external identifier within the workflow type. Generated by nflow if not given.")
  public String externalId;

  @ApiModelProperty("JSON document that contains business information")
  public JsonNode requestData;

  @ApiModelProperty("Start time for workflow execution. If null, defaults to now.")
  public DateTime activationTime;

}