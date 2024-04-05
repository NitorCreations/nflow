package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.joda.time.Period;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Basic information of workflow definition")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ListWorkflowDefinitionResponse extends ModelObject implements Comparable<ListWorkflowDefinitionResponse> {

  @Schema(description = "Type of the workflow definition", requiredMode = REQUIRED)
  public String type;

  @Schema(description = "Name of the workflow definition", requiredMode = REQUIRED)
  public String name;

  @Schema(description = "Description of the workflow definition")
  public String description;

  @Schema(description = "Default error state", requiredMode = REQUIRED)
  public String onError;

  @Schema(description = "Workflow definition states and transitions", requiredMode = REQUIRED)
  public State[] states;

  @Schema(description = "Workflow settings", requiredMode = REQUIRED)
  public Settings settings;

  @Schema(description = "Supported signals")
  public Signal[] supportedSignals;

  public static class Settings extends ModelObject {

    @Schema(description = "Global transition delays for the workflow", requiredMode = REQUIRED)
    public TransitionDelays transitionDelaysInMilliseconds;

    @Schema(description = "Maximum retries for a state before moving to failure", requiredMode = REQUIRED)
    public int maxRetries;

    @Schema(
        description = "Delay after which workflow instance history (actions, states) can be deleted from database. Supports ISO-8601 format.",
        type = "string", format = "duration", example = "PT15D")
    public Period historyDeletableAfter;

    @Schema(description = "Default priority for new workflow instances", requiredMode = REQUIRED)
    public short defaultPriority;

  }

  public static class TransitionDelays extends ModelObject {

    @Schema(description = "Short delay between transitions", requiredMode = REQUIRED)
    public long waitShort;

    @Schema(description = "First retry delay after failure", requiredMode = REQUIRED)
    public long minErrorWait;

    @Schema(description = "Maximum delay between failure retries", requiredMode = REQUIRED)
    public long maxErrorWait;

  }

  public static class Signal extends ModelObject {

    @Schema(description = "Signal value", requiredMode = REQUIRED)
    public int value;

    @Schema(description = "Signal description", requiredMode = REQUIRED)
    public String description;

  }

  @Override
  @SuppressFBWarnings(value = { "EQ_COMPARETO_USE_OBJECT_EQUALS", "WEM_WEAK_EXCEPTION_MESSAGING" },
      justification = "This class has a natural ordering that is inconsistent with equals, exception message is ok")
  public int compareTo(@NonNull ListWorkflowDefinitionResponse response) {
    if (type == null) {
      throw new IllegalStateException("type must be set");
    }
    return type.compareTo(response.type);
  }

}
