package com.nitorcreations.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;

public class StatisticsDaoTest extends BaseDaoTest {

  @Inject
  ExecutorDao executorDao;
  @Inject
  WorkflowInstanceDao instanceDao;
  @Inject
  StatisticsDao statisticsDao;

  @Before
  public void setup() {
    createInstance();
    createInstance();
  }

  @Test
  public void getQueueStatisticsReasonableResults() {
    Statistics stats = statisticsDao.getQueueStatistics();
    QueueStatistics exec = stats.executionStatistics;
    assertThat(exec.count, is(0));
    assertThat(exec.maxAgeMsec, nullValue());
    assertThat(exec.minAgeMsec, nullValue());

    QueueStatistics queued = stats.queuedStatistics;
    assertThat(queued.count, is(2));
    assertThat(queued.maxAgeMsec, greaterThanOrEqualTo(queued.minAgeMsec));
    assertThat(queued.maxAgeMsec, greaterThanOrEqualTo(0l));
    assertThat(queued.minAgeMsec, greaterThanOrEqualTo(0l));
  }

  private int createInstance() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    int id = instanceDao.insertWorkflowInstance(i1);
    return id;
  }
}
