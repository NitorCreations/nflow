package com.nitorcreations.nflow.engine.workflow.statistics;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class Statistics {

  public final QueueStatistics queuedStatistics;
  public final QueueStatistics executionStatistics;

  public Statistics(QueueStatistics queuedStatistics, QueueStatistics executionStatistics) {
    this.queuedStatistics = queuedStatistics;
    this.executionStatistics = executionStatistics;
  }

  public static class QueueStatistics {
    public final int count;
    public final Long maxAgeMsec;
    public final Long minAgeMsec;

    public QueueStatistics(int count, Long maxAgeMsec, Long minAgeMsec) {
      this.count = count;
      this.maxAgeMsec = maxAgeMsec;
      this.minAgeMsec = minAgeMsec;
    }
    @Override
    public String toString() {
      return reflectionToString(this, SHORT_PREFIX_STYLE);
    }
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }
}
