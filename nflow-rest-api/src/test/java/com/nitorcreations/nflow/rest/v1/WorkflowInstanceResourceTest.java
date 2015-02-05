package com.nitorcreations.nflow.rest.v1;

import static com.nitorcreations.Matchers.hasField;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v1.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;

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

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    resource = new WorkflowInstanceResource(workflowInstances, createWorkflowConverter, listWorkflowConverter);
  }

  @Test
  public void createWorkflowInstanceWorks() {
    when(workflowInstances.insertWorkflowInstance(any(WorkflowInstance.class))).thenReturn(1);
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    Response r = resource.createWorkflowInstance(req);
    assertThat(r.getStatus(), is(201));
    assertThat(r.getHeaderString("Location"), is("1"));
    verify(createWorkflowConverter).convertAndValidate(req);
    verify(workflowInstances).insertWorkflowInstance(any(WorkflowInstance.class));
    verify(workflowInstances).getWorkflowInstance(1);
    verify(createWorkflowConverter).convert(any(WorkflowInstance.class));
  }

  @Test
  public void whenUpdatingStateUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance((WorkflowInstance) argThat(hasField("state", equalTo(req.state))),
        (WorkflowInstanceAction) argThat(hasField("stateText", equalTo("API changed state to newState."))));
  }

  @Test
  public void whenUpdatingStateWithDescriptionUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    req.actionDescription = "description";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("state", equalTo(req.state))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingNextActivationTimeUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014,11,12,17,55,0);
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("state", equalTo(null))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("API changed nextActivationTime to "
            + req.nextActivationTime + ".")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingNextActivationTimeWithDescriptionUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014, 11, 12, 17, 55, 0);
    req.actionDescription = "description";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("state", equalTo(null))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingStatusUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.status = "finished";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("status", equalTo(WorkflowInstanceStatus.finished))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("API changed status to finished.")),
            hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingStatusWithDescriptionUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.status = "finished";
    req.actionDescription = "description";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("status", equalTo(WorkflowInstanceStatus.finished))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("description")),
            hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void stoppingWorkflowInstanceWorks() {
    when(workflowInstances.stopWorkflowInstance(3, "test", externalChange)).thenReturn(true);
    Response response = resource.stopWorkflowInstance(3, "test");
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void stoppingWorkflowInstanceWithEmptyActionDescriptionWorks() {
    when(workflowInstances.stopWorkflowInstance(3, "Workflow stopped via API", externalChange)).thenReturn(true);
    Response response = resource.stopWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void stopWorkflowInstanceReturnsErrorWhenStoppingFails() {
    when(workflowInstances.stopWorkflowInstance(3, "Workflow stopped via API", externalChange)).thenReturn(false);
    Response response = resource.stopWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.CONFLICT.getStatusCode()));
  }

  @Test
  public void pausingWorkflowInstanceWorks() {
    when(workflowInstances.pauseWorkflowInstance(3, "test", externalChange)).thenReturn(true);
    Response response = resource.pauseWorkflowInstance(3, "test");
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void pausingWorkflowInstanceWithEmptyActionDescriptionWorks() {
    when(workflowInstances.pauseWorkflowInstance(3, "Workflow paused via API", externalChange)).thenReturn(true);
    Response response = resource.pauseWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void pauseWorkflowInstanceReturnsErrorWhenPausingFails() {
    when(workflowInstances.pauseWorkflowInstance(3, "Workflow paused via API", externalChange)).thenReturn(false);
    Response response = resource.pauseWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.CONFLICT.getStatusCode()));
  }

  @Test
  public void resumingWorkflowInstanceWorks() {
    when(workflowInstances.resumeWorkflowInstance(3, "test", externalChange)).thenReturn(true);
    Response response = resource.resumeWorkflowInstance(3, "test");
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void resumingWorkflowInstanceWithEmptyActionDescriptionWorks() {
    when(workflowInstances.resumeWorkflowInstance(3, "Workflow resumed via API", externalChange)).thenReturn(true);
    Response response = resource.resumeWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
  }

  @Test
  public void resumeWorkflowInstanceReturnsErrorWhenResumingFails() {
    when(workflowInstances.resumeWorkflowInstance(3, "Workflow resumed via API", externalChange)).thenReturn(false);
    Response response = resource.resumeWorkflowInstance(3, null);
    assertThat(response.getStatus(), is(Response.Status.CONFLICT.getStatusCode()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listWorkflowInstancesWorks() {
    resource.listWorkflowInstances(new Integer[] { 42 }, new String[] { "type" }, new String[] { "state" }, "businessKey",
        "externalId", "", 1L);
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", contains("type")),
        hasField("states", contains("state")),
        hasField("businessKey", equalTo("businessKey")),
        hasField("externalId", equalTo("externalId")),
        hasField("includeActions", equalTo(false)),
        hasField("includeCurrentStateVariables", equalTo(false)),
        hasField("includeActionStateVariables", equalTo(false)))));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listWorkflowInstancesWorksWithActionAndStateVariableFetches() {
    resource.listWorkflowInstances(new Integer[] { 42 }, new String[] { "type" }, new String[] { "state" }, "businessKey",
        "externalId", "actions,currentStateVariables,actionStateVariables", 1L);
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", contains("type")),
        hasField("states", contains("state")),
        hasField("businessKey", equalTo("businessKey")),
        hasField("externalId", equalTo("externalId")),
        hasField("includeActions", equalTo(true)),
        hasField("includeCurrentStateVariables", equalTo(true)),
        hasField("includeActionStateVariables", equalTo(true)))));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fetchingNonExistingWorkflowThrowsNotFoundException() {
    thrown.expect(NotFoundException.class);
    QueryWorkflowInstances query = (QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", emptyCollectionOf(String.class)),
        hasField("states", emptyCollectionOf(String.class)),
        hasField("businessKey", equalTo(null)),
        hasField("externalId", equalTo(null)),
        hasField("includeActions", equalTo(true)),
        hasField("includeCurrentStateVariables", equalTo(true)),
        hasField("includeActionStateVariables", equalTo(true))));
    when(workflowInstances.listWorkflowInstances(query)).thenReturn(Collections.<WorkflowInstance>emptyList());
    resource.fetchWorkflowInstance(42);
    verify(workflowInstances).listWorkflowInstances(query);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fetchingExistingWorkflowReturnFirstWorkflowInstance() {
    WorkflowInstance instance1 = mock(WorkflowInstance.class);
    WorkflowInstance instance2 = mock(WorkflowInstance.class);
    QueryWorkflowInstances query = (QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", emptyCollectionOf(String.class)),
        hasField("states", emptyCollectionOf(String.class)),
        hasField("businessKey", equalTo(null)),
        hasField("externalId", equalTo(null)),
        hasField("includeActions", equalTo(true)),
        hasField("includeCurrentStateVariables", equalTo(true)),
        hasField("includeActionStateVariables", equalTo(true))));
    when(workflowInstances.listWorkflowInstances(query)).thenReturn(Arrays.asList(instance1, instance2));
    ListWorkflowInstanceResponse resp1 = mock(ListWorkflowInstanceResponse.class);
    ListWorkflowInstanceResponse resp2 = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance1), any(QueryWorkflowInstances.class))).thenReturn(resp1, resp2);
    ListWorkflowInstanceResponse result = resource.fetchWorkflowInstance(42);
    verify(workflowInstances).listWorkflowInstances(any(QueryWorkflowInstances.class));
    assertEquals(resp1, result);
  }


}
