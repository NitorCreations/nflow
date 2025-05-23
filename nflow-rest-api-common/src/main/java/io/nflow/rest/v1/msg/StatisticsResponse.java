package io.nflow.rest.v1.msg;

import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Response for statistics")
public class StatisticsResponse extends ModelObject {

  @Schema(requiredMode = REQUIRED)
  // Statistics for queued workflows. Workflow instances waiting for free executors.
  public QueueStatistics queueStatistics = new QueueStatistics();

  @Schema(requiredMode = REQUIRED)
  // Statistics for workflows in execution. Workflow instances currently processed by an executor.
  public QueueStatistics executionStatistics = new QueueStatistics();

}
