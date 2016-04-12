package io.nflow.engine.workflow.statistics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
public class Statistics extends ModelObject {

  public final QueueStatistics queuedStatistics;
  public final QueueStatistics executionStatistics;

  public Statistics(QueueStatistics queuedStatistics, QueueStatistics executionStatistics) {
    this.queuedStatistics = queuedStatistics;
    this.executionStatistics = executionStatistics;
  }

  @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
  public static class QueueStatistics extends ModelObject {
    public final int count;
    public final Long maxAgeMillis;
    public final Long minAgeMillis;

    public QueueStatistics(int count, Long maxAgeMillis, Long minAgeMillis) {
      this.count = count;
      this.maxAgeMillis = maxAgeMillis;
      this.minAgeMillis = minAgeMillis;
    }
  }
}
