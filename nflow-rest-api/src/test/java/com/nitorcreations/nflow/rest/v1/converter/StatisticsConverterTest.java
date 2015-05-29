package com.nitorcreations.nflow.rest.v1.converter;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse.StateStatistics;


public class StatisticsConverterTest {
  private final StatisticsConverter converter = new StatisticsConverter();

  @Test
  public void queueStatisticsConvertWorks() {
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

  @Test
  public void workflowDefinitionStatisticsConverterWorks() {
    Map<String, WorkflowDefinitionStatistics> stateStats = new HashMap<>();
    WorkflowDefinitionStatistics created = new WorkflowDefinitionStatistics();
    created.allInstances = 1;
    created.queuedInstances = 2;
    stateStats.put("created", created);
    WorkflowDefinitionStatistics inProgress = new WorkflowDefinitionStatistics();
    inProgress.allInstances = 3;
    inProgress.queuedInstances = 4;
    stateStats.put("inProgress", inProgress);
    WorkflowDefinitionStatistics executing = new WorkflowDefinitionStatistics();
    executing.allInstances = 5;
    stateStats.put("executing", executing);
    WorkflowDefinitionStatistics manual = new WorkflowDefinitionStatistics();
    manual.allInstances = 8;
    stateStats.put("manual", manual);
    WorkflowDefinitionStatistics finished = new WorkflowDefinitionStatistics();
    finished.allInstances = 9;
    stateStats.put("finished", finished);
    stateStats.put("unknown", new WorkflowDefinitionStatistics());
    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = singletonMap("state", stateStats);

    WorkflowDefinitionStatisticsResponse response = converter.convert(stats);

    StateStatistics stateStatistics = response.stateStatistics.get("state");
    assertThat(stateStatistics.created.allInstances, is(1L));
    assertThat(stateStatistics.created.queuedInstances, is(2L));
    assertThat(stateStatistics.inProgress.allInstances, is(3L));
    assertThat(stateStatistics.inProgress.queuedInstances, is(4L));
    assertThat(stateStatistics.executing.allInstances, is(5L));
    assertThat(stateStatistics.manual.allInstances, is(8L));
    assertThat(stateStatistics.finished.allInstances, is(9L));
  }
}
