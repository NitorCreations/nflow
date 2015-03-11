package com.nitorcreations.nflow.rest.v1;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.service.StatisticsService;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.rest.v1.converter.StatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsResourceTest {

  @InjectMocks
  private final StatisticsResource resource = new StatisticsResource();
  @Mock
  private StatisticsService service;
  @Mock
  private StatisticsConverter converter;

  @Before
  public void setup() {
    Map<String, Map<String, WorkflowDefinitionStatistics>> stats = emptyMap();
    when(service.getWorkflowDefinitionStatistics("dummy", null, null, null, null)).thenReturn(stats);
    when(converter.convert(stats)).thenReturn(new WorkflowDefinitionStatisticsResponse());
  }

  @Test
  public void getWorkflowDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = resource.getStatistics("dummy", null, null, null, null);

    verify(service).getWorkflowDefinitionStatistics("dummy", null, null, null, null);
    assertThat(statistics.stateStatistics.size(), is(0));
  }
}
