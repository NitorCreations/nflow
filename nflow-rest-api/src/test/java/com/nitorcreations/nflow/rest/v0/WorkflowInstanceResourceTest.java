package com.nitorcreations.nflow.rest.v0;

import static com.nitorcreations.Matchers.hasField;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.rest.v0.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.UpdateWorkflowInstanceRequest;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceResourceTest {

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private CreateWorkflowConverter createWorkflowConverter;

  @Mock
  private ListWorkflowInstanceConverter listWorkflowConverter;

  @Mock
  private HttpServletResponse httpResponse;

  @Mock
  private WorkflowInstance i;

  private WorkflowInstanceResource resource;

  @Before
  public void setup() {
    resource = new WorkflowInstanceResource(repositoryService, createWorkflowConverter, listWorkflowConverter);
  }

  @Test
  public void createWorkflowInstanceWorks() throws JsonProcessingException {
    when(repositoryService.insertWorkflowInstance(any(WorkflowInstance.class))).thenReturn(1);
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    resource.createWorkflowInstance(req, httpResponse);
    verify(createWorkflowConverter).convertAndValidate(req);
    verify(repositoryService).insertWorkflowInstance(any(WorkflowInstance.class));
    verify(repositoryService).getWorkflowInstance(any(Integer.class));
    verify(createWorkflowConverter).convert(any(WorkflowInstance.class));
  }

  @Test
  public void createWorkflowInstanceRetryWorks() throws JsonProcessingException {
    when(repositoryService.insertWorkflowInstance(any(WorkflowInstance.class))).thenReturn(-1);
    when(repositoryService.listWorkflowInstances(any(QueryWorkflowInstances.class))).thenReturn(asList(i));
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.externalId = "ABC12345";
    resource.createWorkflowInstance(req, httpResponse);
    verify(createWorkflowConverter).convertAndValidate(req);
    verify(repositoryService).insertWorkflowInstance(any(WorkflowInstance.class));
    verify(repositoryService).listWorkflowInstances((QueryWorkflowInstances) argThat(hasField("externalId", equalTo(req.externalId))));
    verify(createWorkflowConverter).convert(any(WorkflowInstance.class));
  }

  @Test
  public void updateWorkflowInstanceWorks() throws JsonProcessingException {
    when(repositoryService.getWorkflowInstance(1)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    resource.updateWorkflowInstance(1, req);
    verify(repositoryService).updateWorkflowInstance((WorkflowInstance) argThat(hasField("state", equalTo(req.state))),
        any(WorkflowInstanceAction.class));
  }

  @Test
  public void listWorkflowInstancesWorks() throws JsonProcessingException {
    resource.listWorkflowInstances(new String[] { "type" }, new String[] { "state" }, "businessKey", "externalId", "actions");
    verify(repositoryService).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
        hasField("types", contains("type")),
        hasField("states", contains("state")),
        hasField("businessKey", equalTo("businessKey")),
        hasField("externalId", equalTo("externalId")),
        hasField("includeActions", equalTo(true)))));
  }

}
