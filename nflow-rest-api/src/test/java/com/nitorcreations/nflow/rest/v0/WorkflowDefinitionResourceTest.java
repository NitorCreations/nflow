package com.nitorcreations.nflow.rest.v0;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowDefinitionConverter;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowDefinitionResourceTest {

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private ListWorkflowDefinitionConverter converter;

  private WorkflowDefinitionResource resource;

  @Before
  public void setup() {
    @SuppressWarnings("unchecked")
    WorkflowDefinition<? extends WorkflowState> def = mock(WorkflowDefinition.class);
    when(def.getType()).thenReturn("dummy");
    doReturn(Arrays.asList(def)).when(repositoryService).getWorkflowDefinitions();
    resource = new WorkflowDefinitionResource(repositoryService, converter);
  }

  @Test
  public void listWorkflowInstancesFindsExistingDefinition() throws JsonProcessingException {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "dummy" } );
    Assert.assertThat(ret.size(), equalTo(1));
  }

  @Test
  public void listWorkflowInstancesNotFindsNonExistentDefinition() throws JsonProcessingException {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "nonexistent" } );
    Assert.assertThat(ret.size(), equalTo(0));
  }

}
