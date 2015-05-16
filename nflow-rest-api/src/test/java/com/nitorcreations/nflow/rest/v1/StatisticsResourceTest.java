package com.nitorcreations.nflow.rest.v1;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.service.StatisticsService;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.rest.v1.converter.StatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsResourceTest {

  @InjectMocks
  private final StatisticsResource resource = new StatisticsResource();
  @Mock
  private StatisticsService service;
  @Mock
  private StatisticsConverter converter;
  @Mock
  StatisticsResponse expected;
  @Mock
  Statistics stats;

  DateTime now = now();
  DateTime createdAfter = now;
  DateTime createdBefore = now.plusMinutes(1);
  DateTime modifiedAfter = now.plusMinutes(2);
  DateTime modifiedBefore = now.plusMinutes(3);

  @Test
  public void queryStatisticsDelegatesToStatisticsService() {
    when(service.queryStatistics()).thenReturn(stats);
    when(converter.convert(stats)).thenReturn(expected);
    StatisticsResponse response = resource.queryStatistics();

    verify(service).queryStatistics();
    assertThat(response, is(response));
  }

  @Test
  public void getWorkflowDefinitionStatisticsDelegatesToStatisticsService() {
    Map<String, Map<String, WorkflowDefinitionStatistics>> statsMap = emptyMap();
    when(service.getWorkflowDefinitionStatistics("dummy", null, null, null, null)).thenReturn(statsMap);
    when(converter.convert(statsMap)).thenReturn(new WorkflowDefinitionStatisticsResponse());

    WorkflowDefinitionStatisticsResponse statistics = resource.getStatistics("dummy", createdAfter, createdBefore,
            modifiedAfter, modifiedBefore);

    verify(service).getWorkflowDefinitionStatistics("dummy", createdAfter, createdBefore, modifiedAfter, modifiedBefore);
    assertThat(statistics.stateStatistics.size(), is(0));
  }
}
