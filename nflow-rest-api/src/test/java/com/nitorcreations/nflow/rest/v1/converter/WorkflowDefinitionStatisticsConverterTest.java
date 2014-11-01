package com.nitorcreations.nflow.rest.v1.converter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

public class WorkflowDefinitionStatisticsConverterTest {

  private final WorkflowDefinitionStatisticsConverter converter = new WorkflowDefinitionStatisticsConverter();

  @Test
  public void converterWorks() {
    Map<String, StateExecutionStatistics> stats = new LinkedHashMap<>();
    StateExecutionStatistics statistics = new StateExecutionStatistics();
    statistics.executing = 1;
    statistics.nonScheduled = 2;
    statistics.queued = 3;
    statistics.sleeping = 4;
    stats.put("test", statistics);

    WorkflowDefinitionStatisticsResponse response = converter.convert(stats);

    Map<String, Long> stateStatistics = response.stateStatistics.get("test");
    assertThat(stateStatistics.get("executing"), is(1L));
    assertThat(stateStatistics.get("nonScheduled"), is(2L));
    assertThat(stateStatistics.get("queued"), is(3L));
    assertThat(stateStatistics.get("sleeping"), is(4L));
  }
}
