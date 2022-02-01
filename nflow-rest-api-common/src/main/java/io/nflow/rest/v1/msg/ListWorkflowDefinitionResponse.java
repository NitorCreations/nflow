package io.nflow.rest.v1.msg;

import org.joda.time.ReadablePeriod;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic information of workflow definition")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ListWorkflowDefinitionResponse extends ModelObject implements Comparable<ListWorkflowDefinitionResponse> {

  @Schema(description = "Type of the workflow definition", required = true)
  public String type;

  @Schema(description = "Name of the workflow definition", required = true)
  public String name;

  @Schema(description = "Description of the workflow definition")
  public String description;

  @Schema(description = "Default error state", required = true)
  public String onError;

  @Schema(description = "Workflow definition states and transitions", required = true)
  public State[] states;

  @Schema(description = "Workflow settings", required = true)
  public Settings settings;

  @Schema(description = "Supported signals")
  public Signal[] supportedSignals;

  public static class Settings extends ModelObject {

    @Schema(description = "Global transition delays for the workflow", required = true)
    public TransitionDelays transitionDelaysInMilliseconds;

    @Schema(description = "Maximum retries for a state before moving to failure", required = true)
    public int maxRetries;

    @Schema(
        description = "Delay after which workflow instance history (actions, states) can be deleted from database. Supports ISO-8601 format.",
        type = "string", format = "duration", example = "PT15D")
    public ReadablePeriod historyDeletableAfter;

    @Schema(description = "Default priority for new workflow instances", required = true)
    public short defaultPriority;

  }

  public static class TransitionDelays extends ModelObject {

    @Schema(description = "Short delay between transitions", required = true)
    public long waitShort;

    @Schema(description = "First retry delay after failure", required = true)
    public long minErrorWait;

    @Schema(description = "Maximum delay between failure retries", required = true)
    public long maxErrorWait;

  }

  public static class Signal extends ModelObject {

    @Schema(description = "Signal value", required = true)
    public int value;

    @Schema(description = "Signal description", required = true)
    public String description;

  }

  @Override
  @SuppressFBWarnings(value = { "EQ_COMPARETO_USE_OBJECT_EQUALS", "WEM_WEAK_EXCEPTION_MESSAGING" },
      justification = "This class has a natural ordering that is inconsistent with equals, exception message is ok")
  public int compareTo(ListWorkflowDefinitionResponse response) {
    if (type == null) {
      throw new IllegalStateException("type must be set");
    }
    return type.compareTo(response.type);
  }

}
