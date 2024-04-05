package io.nflow.rest.v1.msg;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Basic information of workflow executor")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ListWorkflowExecutorResponse extends ModelObject {

  @Schema(description = "Identifier of the workflow executor", requiredMode = REQUIRED)
  public int id;

  @Schema(description = "Host where the executor is running", requiredMode = REQUIRED)
  public String host;

  @Schema(description = "Executor process identifier assigned by the operating system", requiredMode = REQUIRED)
  public int pid;

  @Schema(description = "Executor group the executor belongs to", requiredMode = REQUIRED)
  public String executorGroup;

  @Schema(description = "Time when the executor was started", requiredMode = REQUIRED)
  public DateTime started;

  @Schema(description = "Last time the executor updated it's heart beat to the database", requiredMode = REQUIRED)
  public DateTime active;

  @Schema(description = "Time after which the executor is considered as crashed", requiredMode = REQUIRED)
  public DateTime expires;

  @Schema(description = "Time when the executor was stopped")
  public DateTime stopped;

  @Schema(description = "Time when the workflow instances of a dead node were recovered")
  public DateTime recovered;
}
