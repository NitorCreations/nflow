package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for statistics")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class StatisticsResponse extends ModelObject {

  @Schema(description = "Statistics for queued workflows. Workflow instances waiting for free executors.", required = true)
  public QueueStatistics queueStatistics = new QueueStatistics();

  @Schema(description = "Statistics for workflows in execution. Workflow instances currently processed by an executor.", required = true)
  public QueueStatistics executionStatistics = new QueueStatistics();

}
