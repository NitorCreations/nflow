package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Global statistics for workflow instances.")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class QueueStatistics extends ModelObject {

  @Schema(description = "Number for workflow instances.", required = true)
  public int count;
  @Schema(description = "Maximum time (ms) currently queued workflow instances have been in queue.")
  public Long maxAge;
  @Schema(description = "Minimum time (ms) currently queued workflow instances have been in queue.")
  public Long minAge;

}
