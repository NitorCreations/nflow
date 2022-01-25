package io.nflow.rest.v1.msg;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "State change attempt. A new instance for every retry attempt.")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class Action extends ModelObject {

  @Schema(description = "Identifier of the workflow instance action", required = true)
  public long id;
  @Schema(description = "Type of state", required = true, allowableValues = { "stateExecution", "stateExecutionFailed",
      "externalChange", "recovery" })
  public String type;
  @Schema(description = "Name of state", required = true)
  public String state;
  @Schema(description = "Description of state")
  public String stateText;
  @Schema(description = "Number of retries in this state")
  public int retryNo;
  @Schema(description = "Start time for execution")
  public DateTime executionStartTime;
  @Schema(description = "End time for execution")
  public DateTime executionEndTime;
  @Schema(description = "Identifier of the executor that executed this action")
  public int executorId;
  @Schema(description = "Updated state variables")
  public Map<String, Object> updatedStateVariables;

  public Action() {
  }

  public Action(long id, @NonNull String type, @NonNull String state, String stateText, int retryNo, DateTime executionStartTime,
      DateTime executionEndTime, int executorId) {
    this(id, type, state, stateText, retryNo, executionStartTime, executionEndTime, executorId, null);
  }

  public Action(long id, @NonNull String type, @NonNull String state, String stateText, int retryNo, DateTime executionStartTime,
      DateTime executionEndTime, int executorId, Map<String, Object> updatedStateVariables) {
    this();
    this.id = id;
    this.type = type;
    this.state = state;
    this.stateText = stateText;
    this.retryNo = retryNo;
    this.executionStartTime = executionStartTime;
    this.executionEndTime = executionEndTime;
    this.executorId = executorId;
    this.updatedStateVariables = updatedStateVariables;
  }
}
