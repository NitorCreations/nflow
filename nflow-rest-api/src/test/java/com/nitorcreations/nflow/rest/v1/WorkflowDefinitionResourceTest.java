package com.nitorcreations.nflow.rest.v1;

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

import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowDefinitionResourceTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitions;

  @Mock
  private ListWorkflowDefinitionConverter converter;

  private WorkflowDefinitionResource resource;

  @Before
  public void setup() {
    @SuppressWarnings("unchecked")
    WorkflowDefinition<? extends WorkflowState> def = mock(WorkflowDefinition.class);
    when(def.getType()).thenReturn("dummy");
    doReturn(Arrays.asList(def)).when(workflowDefinitions).getWorkflowDefinitions();
    resource = new WorkflowDefinitionResource(workflowDefinitions, converter);
  }

  @Test
  public void listWorkflowInstancesFindsExistingDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "dummy" } );
    Assert.assertThat(ret.size(), equalTo(1));
  }

  @Test
  public void listWorkflowInstancesNotFindsNonExistentDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowInstances(new String[] { "nonexistent" } );
    Assert.assertThat(ret.size(), equalTo(0));
  }

}
