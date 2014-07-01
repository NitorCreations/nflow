package com.nitorcreations.nflow.rest.v0;

import static com.nitorcreations.Matchers.hasField;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v0.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.UpdateWorkflowInstanceRequest;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceResourceTest {

  @Mock
  private WorkflowInstanceService workflowInstances;

  @Mock
  private CreateWorkflowConverter createWorkflowConverter;

  @Mock
  private ListWorkflowInstanceConverter listWorkflowConverter;

  private final WorkflowInstance i = new WorkflowInstance.Builder().setId(2).build();

  private WorkflowInstanceResource resource;

  @Before
  public void setup() {
    resource = new WorkflowInstanceResource(workflowInstances, createWorkflowConverter, listWorkflowConverter);
  }

  @Test
  public void createWorkflowInstanceWorks() throws JsonProcessingException {
    when(workflowInstances.insertWorkflowInstance(any(WorkflowInstance.class))).thenReturn(1);
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    Response r = resource.createWorkflowInstance(req);
    assertThat(r.getStatus(), is(201));
    assertThat(r.getHeaderString("Location"), is("1"));
    verify(createWorkflowConverter).convertAndValidate(req);
    verify(workflowInstances).insertWorkflowInstance(any(WorkflowInstance.class));
    verify(workflowInstances).getWorkflowInstance(any(Integer.class));
    verify(createWorkflowConverter).convert(any(WorkflowInstance.class));
  }

  @Test
  public void createWorkflowInstanceRetryWorks() throws JsonProcessingException {
    when(workflowInstances.insertWorkflowInstance(any(WorkflowInstance.class))).thenReturn(-1);
    when(workflowInstances.listWorkflowInstances(any(QueryWorkflowInstances.class))).thenReturn(asList(i));
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.externalId = "ABC12345";
    Response r = resource.createWorkflowInstance(req);
    assertThat(r.getStatus(), is(201));
    assertThat(r.getHeaderString("Location"), is("2"));
    verify(createWorkflowConverter).convertAndValidate(req);
    verify(workflowInstances).insertWorkflowInstance(any(WorkflowInstance.class));
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(hasField("externalId", equalTo(req.externalId))));
    verify(createWorkflowConverter).convert(any(WorkflowInstance.class));
  }

  @Test
  public void updateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance((WorkflowInstance) argThat(hasField("state", equalTo(req.state))),
        any(WorkflowInstanceAction.class));
  }

  @Test
  public void listWorkflowInstancesWorks() {
    resource.listWorkflowInstances(new Integer[]{42}, new String[] { "type" }, new String[] { "state" }, "businessKey", "externalId", "actions");
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", contains("type")),
        hasField("states", contains("state")),
        hasField("businessKey", equalTo("businessKey")),
        hasField("externalId", equalTo("externalId")),
        hasField("includeActions", equalTo(true)))));
  }


  @Test
  public void fetchWorkflowInstancesWorks() {
    resource.fetchWorkflowInstance(42);
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", emptyCollectionOf(String.class)),
        hasField("states", emptyCollectionOf(String.class)),
        hasField("businessKey", equalTo(null)),
        hasField("externalId", equalTo(null)),
        hasField("includeActions", equalTo(true)))));
  }

}
