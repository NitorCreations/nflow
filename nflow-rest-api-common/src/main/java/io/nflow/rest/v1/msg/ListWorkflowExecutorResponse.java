package io.nflow.rest.v1.msg;

import io.swagger.v3.oas.annotations.media.Schema;
import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@Schema(description = "Basic information of workflow executor")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class ListWorkflowExecutorResponse extends ModelObject {

  @Schema(description = "Identifier of the workflow executor", required=true)
  public int id;

  @Schema(description = "Host where the executor is running", required=true)
  public String host;

  @Schema(description = "Executor process identifier assigned by the operating system", required=true)
  public int pid;

  @Schema(description = "Executor group the executor belongs to", required=true)
  public String executorGroup;

  @Schema(description = "Time when the executor was started", required=true)
  public DateTime started;

  @Schema(description = "Last time the executor updated it's heart beat to the database", required=true)
  public DateTime active;

  @Schema(description = "Time after which the executor is considered as crashed", required=true)
  public DateTime expires;

  @Schema(description = "Time when the executor was stopped", required=true)
  public DateTime stopped;
}
