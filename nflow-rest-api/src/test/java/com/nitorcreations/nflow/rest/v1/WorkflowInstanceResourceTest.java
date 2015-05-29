package com.nitorcreations.nflow.rest.v1;

import static com.nitorcreations.Matchers.hasField;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.util.Arrays.asList;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  public void whenUpdatingWithoutParametersNothingHappens() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances, never()).updateWorkflowInstance(any(WorkflowInstance.class), any(WorkflowInstanceAction.class));
  }

  @Test
  public void whenUpdatingMessageStateTextIsUpdated() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.actionDescription = "my desc";
    resource.updateWorkflowInstance(3, req);

    verify(workflowInstances).updateWorkflowInstance(
            (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
            (WorkflowInstanceAction) argThat(hasField("stateText", equalTo("my desc"))));
  }

  @Test
  public void whenUpdatingStateUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
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
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingNextActivationTimeUpdateWorkflowInstanceWorks() {
    when(workflowInstances.getWorkflowInstance(3)).thenReturn(i);
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014,11,12,17,55,0);
    resource.updateWorkflowInstance(3, req);
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(null)), hasField("status", equalTo(null)))),
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
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(null)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listWorkflowInstancesWorks() {
    resource.listWorkflowInstances(new Integer[] { 42 }, new String[] { "type" }, 99, 88, new String[] { "state" },
        new WorkflowInstance.WorkflowInstanceStatus[] { WorkflowInstanceStatus.created }, "businessKey", "externalId", "", 1L);
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
      hasField("ids", contains(42)),
      hasField("types", contains("type")),
      hasField("parentWorkflowId", is(99)),
      hasField("parentActionId", is(88)),
      hasField("states", contains("state")),
      hasField("statuses", contains(WorkflowInstanceStatus.created)),
      hasField("businessKey", equalTo("businessKey")),
      hasField("externalId", equalTo("externalId")),
      hasField("includeActions", equalTo(false)),
      hasField("includeCurrentStateVariables", equalTo(false)),
      hasField("includeActionStateVariables", equalTo(false)),
      hasField("includeChildWorkflows", equalTo(false)))));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listWorkflowInstancesWorksWithAllIncludes() {
    resource.listWorkflowInstances(new Integer[] { 42 }, new String[] { "type" }, 99, 88, new String[] { "state" },
        new WorkflowInstance.WorkflowInstanceStatus[] { WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing },
        "businessKey", "externalId", "actions,currentStateVariables,actionStateVariables,childWorkflows", 1L);
    verify(workflowInstances).listWorkflowInstances((QueryWorkflowInstances) argThat(allOf(
      hasField("ids", contains(42)),
      hasField("types", contains("type")),
      hasField("parentWorkflowId", is(99)),
      hasField("parentActionId", is(88)),
      hasField("states", contains("state")),
      hasField("statuses", contains(WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing)),
      hasField("businessKey", equalTo("businessKey")),
      hasField("externalId", equalTo("externalId")),
      hasField("includeActions", equalTo(true)),
      hasField("includeCurrentStateVariables", equalTo(true)),
      hasField("includeActionStateVariables", equalTo(true)),
      hasField("includeChildWorkflows", equalTo(true)))));
  }

  @Test
  public void fetchingNonExistingWorkflowThrowsNotFoundException() {
    thrown.expect(NotFoundException.class);
    when(workflowInstances.listWorkflowInstances(any(QueryWorkflowInstances.class))).thenReturn(
        Collections.<WorkflowInstance> emptyList());
    resource.fetchWorkflowInstance(42, null);
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
        hasField("includeActions", equalTo(false)),
        hasField("includeCurrentStateVariables", equalTo(false)),
        hasField("includeActionStateVariables", equalTo(false)),
        hasField("includeChildWorkflows", equalTo(false))));
    when(workflowInstances.listWorkflowInstances(query)).thenReturn(asList(instance1, instance2));
    ListWorkflowInstanceResponse resp1 = mock(ListWorkflowInstanceResponse.class);
    ListWorkflowInstanceResponse resp2 = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance1), any(QueryWorkflowInstances.class))).thenReturn(resp1, resp2);
    ListWorkflowInstanceResponse result = resource.fetchWorkflowInstance(42, null);
    verify(workflowInstances).listWorkflowInstances(any(QueryWorkflowInstances.class));
    assertEquals(resp1, result);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fetchingExistingWorkflowWorksWithAllIncludes() {
    WorkflowInstance instance1 = mock(WorkflowInstance.class);
    QueryWorkflowInstances query = (QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42)),
        hasField("types", emptyCollectionOf(String.class)),
        hasField("states", emptyCollectionOf(String.class)),
        hasField("businessKey", equalTo(null)),
        hasField("externalId", equalTo(null)),
        hasField("includeActions", equalTo(true)),
        hasField("includeCurrentStateVariables", equalTo(true)),
        hasField("includeActionStateVariables", equalTo(true)),
        hasField("includeChildWorkflows", equalTo(true))));
    when(workflowInstances.listWorkflowInstances(query)).thenReturn(asList(instance1));
    ListWorkflowInstanceResponse resp1 = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance1), any(QueryWorkflowInstances.class))).thenReturn(resp1);
    ListWorkflowInstanceResponse result = resource.fetchWorkflowInstance(42, "actions,currentStateVariables,actionStateVariables,childWorkflows");
    verify(workflowInstances).listWorkflowInstances(any(QueryWorkflowInstances.class));
    assertEquals(resp1, result);
  }
}
