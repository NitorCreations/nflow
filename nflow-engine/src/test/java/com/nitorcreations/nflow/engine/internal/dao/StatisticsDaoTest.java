package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;

public class StatisticsDaoTest extends BaseDaoTest {

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
    assertThat(exec.maxAgeMillis, nullValue());
    assertThat(exec.minAgeMillis, nullValue());

    QueueStatistics queued = stats.queuedStatistics;
    assertThat(queued.count, is(2));
    assertThat(queued.maxAgeMillis, greaterThanOrEqualTo(queued.minAgeMillis));
    assertThat(queued.maxAgeMillis, greaterThanOrEqualTo(0l));
    assertThat(queued.minAgeMillis, greaterThanOrEqualTo(0l));
  }

  private int createInstance() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    int id = instanceDao.insertWorkflowInstance(i1);
    return id;
  }

  @Test
  public void getWorkflowDefinitionStatisticsWorks() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).setStatus(created).build();
    int id = instanceDao.insertWorkflowInstance(i1);

    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = statisticsDao.getWorkflowDefinitionStatistics(i1.type, null,
        null, null, null);
    Map<String, WorkflowDefinitionStatistics> stateStats = stats.get("CreateLoan");
    assertThat(stateStats.get(executing.name()), is(nullValue()));
    assertThat(stateStats.get(inProgress.name()).queuedInstances, is(2L));
    assertThat(stateStats.get(inProgress.name()).allInstances, is(2L));
    assertThat(stateStats.get(created.name()).queuedInstances, is(0L));
    assertThat(stateStats.get(created.name()).allInstances, is(1L));

    WorkflowInstance i2 = new WorkflowInstance.Builder().setId(id).setNextActivation(now().minusDays(1)).build();
    instanceDao.updateNotRunningWorkflowInstance(i2);

    stats = statisticsDao.getWorkflowDefinitionStatistics(i1.type, null, null, null, null);
    stateStats = stats.get("CreateLoan");
    assertThat(stateStats.get(executing.name()), is(nullValue()));
    assertThat(stateStats.get(inProgress.name()).queuedInstances, is(2L));
    assertThat(stateStats.get(inProgress.name()).allInstances, is(2L));
    assertThat(stateStats.get(created.name()).queuedInstances, is(1L));
    assertThat(stateStats.get(created.name()).allInstances, is(1L));
  }

  @Test
  public void getStatisticsWorksWithCreatedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    instanceDao.insertWorkflowInstance(i1);

    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = statisticsDao.getWorkflowDefinitionStatistics(i1.type, now()
        .plusDays(1), now().plusDays(2), null, null);

    assertThat(stats.size(), is(0));
  }

  @Test
  public void getStatisticsWorksWithModifiedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    instanceDao.insertWorkflowInstance(i1);

    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = statisticsDao.getWorkflowDefinitionStatistics(i1.type, null,
        null, now().plusDays(1), now().plusDays(2));

    assertThat(stats.size(), is(0));
  }

  @Test
  public void getStatisticsWorksWithCreatedAndModifiedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    instanceDao.insertWorkflowInstance(i1);

    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = statisticsDao.getWorkflowDefinitionStatistics(i1.type, now()
        .minusDays(1), now().plusDays(1), now().minusDays(1), now().plusDays(1));

    assertThat(stats.size(), is(1));
  }
}
