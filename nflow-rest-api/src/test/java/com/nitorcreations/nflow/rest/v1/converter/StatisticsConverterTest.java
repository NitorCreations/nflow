package com.nitorcreations.nflow.rest.v1.converter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;


public class StatisticsConverterTest {
  private final StatisticsConverter converter = new StatisticsConverter();

  @Test
  public void convertWorks() {
    QueueStatistics queuedStatistics = new QueueStatistics(2, 42l, 10l);
    QueueStatistics executionStatistics = new QueueStatistics(5, null, null);
    Statistics stats = new Statistics(queuedStatistics, executionStatistics);
    StatisticsResponse response = converter.convert(stats);

    com.nitorcreations.nflow.rest.v1.msg.QueueStatistics restQueueStats =  response.queueStatistics;
    assertThat(restQueueStats.count, is(2));
    assertThat(restQueueStats.maxAge, is(42l));
    assertThat(restQueueStats.minAge, is(10l));

    com.nitorcreations.nflow.rest.v1.msg.QueueStatistics restExecStats =  response.executionStatistics;
    assertThat(restExecStats.count, is(5));
    assertThat(restExecStats.maxAge, nullValue());
    assertThat(restExecStats.minAge, nullValue());
  }
}
