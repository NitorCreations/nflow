package com.nitorcreations.nflow.engine.workflow.statistics;

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
  }
}
