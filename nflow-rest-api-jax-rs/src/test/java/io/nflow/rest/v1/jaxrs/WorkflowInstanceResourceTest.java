package io.nflow.rest.v1.jaxrs;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.v1.converter.CreateWorkflowConverter;
import io.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.ErrorResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.SetSignalRequest;
import io.nflow.rest.v1.msg.SetSignalResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;

@ExtendWith(MockitoExtension.class)
public class WorkflowInstanceResourceTest {

  @Mock
  private WorkflowInstanceService workflowInstances;

  @Mock
  private CreateWorkflowConverter createWorkflowConverter;

  @Mock
  private ListWorkflowInstanceConverter listWorkflowConverter;

  @Mock
  private WorkflowInstanceFactory workflowInstanceFactory;

  @Mock
  private WorkflowInstanceDao workflowInstanceDao;

  private WorkflowInstanceResource resource;

  @Captor
  private ArgumentCaptor<WorkflowInstance> instanceCaptor;

  @Captor
  private ArgumentCaptor<WorkflowInstanceAction> actionCaptor;

  @Captor
  private ArgumentCaptor<QueryWorkflowInstances> queryCaptor;

  @BeforeEach
  public void setup() {
    resource = new WorkflowInstanceResource(workflowInstances, createWorkflowConverter, listWorkflowConverter,
        workflowInstanceFactory, workflowInstanceDao);
    lenient().when(workflowInstanceFactory.newWorkflowInstanceBuilder())
    .thenReturn(new WorkflowInstance.Builder(new ObjectStringMapper(new ObjectMapper())));
  }

  @Test
  public void createWorkflowInstanceWorks() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    WorkflowInstance inst = mock(WorkflowInstance.class);
    when(createWorkflowConverter.convert(req)).thenReturn(inst);
    when(workflowInstances.insertWorkflowInstance(inst)).thenReturn(1L);
    try (Response r = resource.createWorkflowInstance(req)) {
      assertThat(r.getStatus(), is(CREATED.getStatusCode()));
      assertThat(r.getHeaderString("Location"), is("1"));
      verify(createWorkflowConverter).convert(req);
      verify(workflowInstances).insertWorkflowInstance(any(WorkflowInstance.class));
      verify(workflowInstances).getWorkflowInstance(1, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
    }
  }

  @Test
  public void whenUpdatingWithoutParametersNothingHappens() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances, never()).updateWorkflowInstance(any(WorkflowInstance.class), any(WorkflowInstanceAction.class));
  }

  @Test
  public void whenUpdatingMessageStateTextIsUpdated() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.actionDescription = "my desc";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.state, is(req.state));
    assertThat(instance.status, is(nullValue()));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is(req.actionDescription));
  }

  @Test
  public void whenUpdatingStateUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.state, is(req.state));
    assertThat(instance.status, is(nullValue()));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is("API changed state to newState."));
  }

  @Test
  public void whenUpdatingStateWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    req.actionDescription = "description";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.state, is(req.state));
    assertThat(instance.status, is(nullValue()));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is(req.actionDescription));
    assertThat(action.type, is(externalChange));
  }

  @Test
  public void whenUpdatingNextActivationTimeUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014, 11, 12, 17, 55, 0);

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.state, is(nullValue()));
    assertThat(instance.status, is(nullValue()));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is("API changed nextActivationTime to " + req.nextActivationTime + "."));
    assertThat(action.type, is(externalChange));
  }

  @Test
  public void whenUpdatingNextActivationTimeWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014, 11, 12, 17, 55, 0);
    req.actionDescription = "description";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.state, is(nullValue()));
    assertThat(instance.status, is(nullValue()));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is(req.actionDescription));
    assertThat(action.type, is(externalChange));
  }

  @Test
  public void whenUpdatingStateVariablesUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.stateVariables.put("foo", "bar");
    req.stateVariables.put("textNode", new TextNode("text"));

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.getStateVariable("foo"), is("bar"));
    assertThat(instance.getStateVariable("textNode"), is("\"text\""));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is("API updated state variables."));
  }

  @Test
  public void whenUpdatingBusinessKeyUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.businessKey = "modifiedKey";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.businessKey, is(req.businessKey));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is("API changed business key to " + req.businessKey + "."));
    assertThat(action.type, is(externalChange));
  }

  @Test
  public void whenUpdatingBusinessKeyWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.businessKey = "modifiedKey";
    req.actionDescription = "description";

    makeRequest(() -> resource.updateWorkflowInstance(3, req));

    verify(workflowInstances).updateWorkflowInstance(instanceCaptor.capture(), actionCaptor.capture());
    WorkflowInstance instance = instanceCaptor.getValue();
    assertThat(instance.businessKey, is(req.businessKey));
    WorkflowInstanceAction action = actionCaptor.getValue();
    assertThat(action.stateText, is(req.actionDescription));
    assertThat(action.type, is(externalChange));
  }

  @Test
  public void listWorkflowInstancesWorks() {
    makeRequest(() -> resource.listWorkflowInstances(asList(42L), asList("type"), 99L, 88L, asList("state"),
        asList(WorkflowInstanceStatus.created), "businessKey", "externalId", null, null, "", null, null, true));

    verify(workflowInstances).listWorkflowInstancesAsStream(queryCaptor.capture());
    QueryWorkflowInstances query = queryCaptor.getValue();
    assertThat(query.ids, contains(42L));
    assertThat(query.types, contains("type"));
    assertThat(query.parentWorkflowId, is(99L));
    assertThat(query.parentActionId, is(88L));
    assertThat(query.states, contains("state"));
    assertThat(query.statuses, contains(WorkflowInstanceStatus.created));
    assertThat(query.businessKey, equalTo("businessKey"));
    assertThat(query.externalId, equalTo("externalId"));
    assertThat(query.stateVariableKey, nullValue());
    assertThat(query.stateVariableValue, nullValue());
    assertThat(query.includeActions, is(false));
    assertThat(query.includeCurrentStateVariables, is(false));
    assertThat(query.includeActionStateVariables, is(false));
    assertThat(query.includeChildWorkflows, is(false));
    assertThat(query.maxResults, is(nullValue()));
    assertThat(query.maxActions, is(nullValue()));
    assertThat(query.queryArchive, is(true));
  }

  @Test
  public void listWorkflowInstancesWorksWithAllIncludes() {
    makeRequest(() -> resource.listWorkflowInstances(asList(42L), asList("type"), 99L, 88L, asList("state"),
        asList(WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing), "businessKey", "externalId",
        "stateVarKey", "stateVarValue", "actions,currentStateVariables,actionStateVariables,childWorkflows", 1L, 2L, false));

    verify(workflowInstances).listWorkflowInstancesAsStream(queryCaptor.capture());
    QueryWorkflowInstances query = queryCaptor.getValue();
    assertThat(query.ids, contains(42L));
    assertThat(query.types, contains("type"));
    assertThat(query.parentWorkflowId, is(99L));
    assertThat(query.parentActionId, is(88L));
    assertThat(query.states, contains("state"));
    assertThat(query.statuses, contains(WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing));
    assertThat(query.businessKey, equalTo("businessKey"));
    assertThat(query.externalId, equalTo("externalId"));
    assertThat(query.stateVariableKey, equalTo("stateVarKey"));
    assertThat(query.stateVariableValue, equalTo("stateVarValue"));
    assertThat(query.includeActions, is(true));
    assertThat(query.includeCurrentStateVariables, is(true));
    assertThat(query.includeActionStateVariables, is(true));
    assertThat(query.includeChildWorkflows, is(true));
    assertThat(query.maxResults, is(1L));
    assertThat(query.maxActions, is(2L));
    assertThat(query.queryArchive, is(false));
  }

  @Test
  public void fetchingNonExistingWorkflowReturnsNotFound() {
    when(workflowInstances.getWorkflowInstance(42, emptySet(), null, true))
    .thenThrow(new NflowNotFoundException("Workflow instance", 42, new Exception()));
    try (Response response = resource.fetchWorkflowInstance(42, null, null, true)) {
      assertThat(response.getStatus(), is(equalTo(NOT_FOUND.getStatusCode())));
      assertThat(response.readEntity(ErrorResponse.class).error, is(equalTo("Workflow instance 42 not found")));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fetchingExistingWorkflowWorks() {
    WorkflowInstance instance = mock(WorkflowInstance.class);
    when(workflowInstances.getWorkflowInstance(42, emptySet(), null, false)).thenReturn(instance);
    ListWorkflowInstanceResponse resp = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance), any(Set.class), eq(false))).thenReturn(resp);
    ListWorkflowInstanceResponse result = getEntity(() -> resource.fetchWorkflowInstance(42, null, null, false),
        ListWorkflowInstanceResponse.class);
    verify(workflowInstances).getWorkflowInstance(42, emptySet(), null, false);
    assertEquals(resp, result);
  }

  @SuppressWarnings({ "unchecked" })
  @Test
  public void fetchingExistingWorkflowWorksWithAllIncludes() {
    WorkflowInstance instance = mock(WorkflowInstance.class);
    EnumSet<WorkflowInstanceInclude> includes = EnumSet.allOf(WorkflowInstanceInclude.class);
    when(workflowInstances.getWorkflowInstance(42, includes, 10L, false)).thenReturn(instance);
    ListWorkflowInstanceResponse resp = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance), any(Set.class), eq(false))).thenReturn(resp);
    ListWorkflowInstanceResponse result = getEntity(
        () -> resource.fetchWorkflowInstance(42, "actions,currentStateVariables,actionStateVariables,childWorkflows", 10L, false),
        ListWorkflowInstanceResponse.class);
    verify(workflowInstances).getWorkflowInstance(42, includes, 10L, false);
    assertEquals(resp, result);
  }

  @Test
  public void setSignalSuccessIsTrueWhenSignalWasSet() {
    SetSignalRequest req = new SetSignalRequest();
    req.signal = 42;
    req.reason = "testing";
    when(workflowInstances.setSignal(99, Optional.of(42), "testing", WorkflowActionType.externalChange)).thenReturn(true);

    SetSignalResponse response = getEntity(() -> resource.setSignal(99, req), SetSignalResponse.class);

    verify(workflowInstances).setSignal(99, Optional.of(42), "testing", WorkflowActionType.externalChange);
    assertTrue(response.setSignalSuccess);
  }

  @Test
  public void setSignalSuccessIsFalseWhenSignalWasNotSet() {
    SetSignalRequest req = new SetSignalRequest();
    req.signal = null;
    req.reason = "testing";
    when(workflowInstances.setSignal(99, Optional.empty(), "testing", WorkflowActionType.externalChange)).thenReturn(false);

    SetSignalResponse response = getEntity(() -> resource.setSignal(99, req), SetSignalResponse.class);

    verify(workflowInstances).setSignal(99, Optional.empty(), "testing", WorkflowActionType.externalChange);
    assertFalse(response.setSignalSuccess);
  }

  private int makeRequest(Supplier<Response> supplier) {
    try (Response r = supplier.get()) {
      return r.getStatus();
    }
  }

  private <T> T getEntity(Supplier<Response> supplier, Class<T> entityClass) {
    try (Response r = supplier.get()) {
      return r.readEntity(entityClass);
    }
  }
}
