package com.nitorcreations.nflow.rest.v1;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import com.nitorcreations.nflow.rest.v1.converter.WorkflowDefinitionStatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowDefinitionResourceTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitions;

  @Mock
  private ListWorkflowDefinitionConverter converter;

  @Mock
  private WorkflowDefinitionStatisticsConverter statisticsConverter;

  private WorkflowDefinitionResource resource;

  @Before
  public void setup() {
    @SuppressWarnings("unchecked")
    WorkflowDefinition<? extends WorkflowState> def = mock(WorkflowDefinition.class);
    when(def.getType()).thenReturn("dummy");
    doReturn(asList(def)).when(workflowDefinitions).getWorkflowDefinitions();
    Map<String, StateExecutionStatistics> stats = emptyMap();
    when(workflowDefinitions.getStatistics("dummy", null, null)).thenReturn(stats);
    when(statisticsConverter.convert(stats)).thenReturn(new WorkflowDefinitionStatisticsResponse());
    resource = new WorkflowDefinitionResource(workflowDefinitions, converter, statisticsConverter);
  }

  @Test
  public void listWorkflowInstancesFindsExistingDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "dummy" } );
    assertThat(ret.size(), is(1));
  }

  @Test
  public void listWorkflowInstancesNotFindsNonExistentDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "nonexistent" } );
    assertThat(ret.size(), is(0));
  }

  @Test
  public void getWorkflowDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = resource.getStatistics("dummy", null, null);
    assertThat(statistics.stateStatistics.size(), is(0));
  }
}
