package io.nflow.rest.v1.msg;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic information of workflow instance")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
@SuppressWarnings("this-escape")
public class ListWorkflowInstanceResponse extends ModelObject {

  @Schema(description = "Identifier of the workflow instance", requiredMode = REQUIRED)
  public long id;

  @Schema(description = "Workflow instance status", requiredMode = REQUIRED, allowableValues = { "created", "executing", "inProgress",
      "finished", "manual" })
  public String status;

  @Schema(description = "Workflow definition type", requiredMode = REQUIRED)
  public String type;

  @Schema(
      description = "Workflow instance priority. Larger value gets (unfair) priority in scheduling. Can be negative.",
      requiredMode = REQUIRED)
  public Short priority;

  @Schema(description = "Parent workflow instance id for child workflows")
  public Long parentWorkflowId;

  @Schema(description = "Parent workflow instance action id for child workflows (action that created the child workflow)")
  public Long parentActionId;

  @Schema(description = "Main business key or identifier for the workflow instance")
  public String businessKey;

  @Schema(description = "Unique external identifier within a workflow type. Generated by nFlow if not given.", requiredMode = REQUIRED)
  public String externalId;

  @Schema(description = "State of the workflow instance", requiredMode = REQUIRED)
  public String state;

  @Schema(description = "Text of describing the reason for state (free text)")
  public String stateText;

  @Schema(description = "Time when the workflow instance is processed again")
  public DateTime nextActivation;

  @Schema(description = "State variables for current state")
  public Map<String, Object> stateVariables;

  @Schema(description = "Number of times the current state has been retried", requiredMode = REQUIRED)
  public int retries;

  @Schema(description = "Action history of the workflow instance")
  @JsonInclude(NON_NULL)
  public List<Action> actions;

  @Schema(description = "Workflow instance creation timestamp", requiredMode = REQUIRED)
  public DateTime created;

  @Schema(description = "Workflow instance latest modification timestamp", requiredMode = REQUIRED)
  public DateTime modified;

  @Schema(description = "Time when workflow processing started (=start time of the first action)")
  public DateTime started;

  @Schema(description = "Child workflow instance IDs created by this instance, grouped by instance action ID")
  public Map<Long, List<Long>> childWorkflows;

  @Schema(description = "Current signal value")
  public Integer signal;

  @Schema(description = "True if the instance is stored in the archive tables")
  public Boolean isArchived;

}
