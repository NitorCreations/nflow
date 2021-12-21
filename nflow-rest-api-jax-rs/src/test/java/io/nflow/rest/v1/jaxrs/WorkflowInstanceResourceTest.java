package io.nflow.rest.v1.jaxrs;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.rest.v1.jaxrs.WorkflowInstanceResourceTest.FieldMatcher.hasField;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
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
  private ArgumentCaptor<WorkflowInstance> workflowInstanceCaptor;

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

    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(hasField("stateText", equalTo("my desc"))));
  }

  @Test
  public void whenUpdatingStateUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(hasField("stateText", equalTo("API changed state to newState."))));
  }

  @Test
  public void whenUpdatingStateWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.state = "newState";
    req.actionDescription = "description";
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(req.state)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(
            allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingNextActivationTimeUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014, 11, 12, 17, 55, 0);
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(null)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(
            allOf(hasField("stateText", equalTo("API changed nextActivationTime to " + req.nextActivationTime + ".")),
                hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingNextActivationTimeWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = new DateTime(2014, 11, 12, 17, 55, 0);
    req.actionDescription = "description";
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(allOf(hasField("state", equalTo(null)), hasField("status", equalTo(null)))),
        (WorkflowInstanceAction) argThat(
            allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingStateVariablesUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.stateVariables.put("foo", "bar");
    req.stateVariables.put("textNode", new TextNode("text"));
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(workflowInstanceCaptor.capture(),
        (WorkflowInstanceAction) argThat(hasField("stateText", equalTo("API updated state variables."))));
    WorkflowInstance instance = workflowInstanceCaptor.getValue();
    assertThat(instance.getStateVariable("foo"), is("bar"));
    assertThat(instance.getStateVariable("textNode"), is("\"text\""));
  }

  @Test
  public void whenUpdatingBusinessKeyUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.businessKey = "modifiedKey";
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("businessKey", equalTo("modifiedKey"))),
        (WorkflowInstanceAction) argThat(
            allOf(hasField("stateText", equalTo("API changed business key to " + req.businessKey + ".")),
                hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void whenUpdatingBusinessKeyWithDescriptionUpdateWorkflowInstanceWorks() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.businessKey = "modifiedKey";
    req.actionDescription = "description";
    makeRequest(() -> resource.updateWorkflowInstance(3, req));
    verify(workflowInstances).updateWorkflowInstance(
        (WorkflowInstance) argThat(hasField("businessKey", equalTo("modifiedKey"))),
        (WorkflowInstanceAction) argThat(
            allOf(hasField("stateText", equalTo("description")), hasField("type", equalTo(externalChange)))));
  }

  @Test
  public void listWorkflowInstancesWorks() {
    makeRequest(() -> resource.listWorkflowInstances(asList(42L), asList("type"), 99L, 88L, asList("state"),
        asList(WorkflowInstanceStatus.created), "businessKey", "externalId", null, null, "", null, null));
    verify(workflowInstances).listWorkflowInstancesAsStream((QueryWorkflowInstances) argThat(allOf(
        hasField("ids", contains(42L)),
        hasField("types", contains("type")),
        hasField("parentWorkflowId", is(99L)),
        hasField("parentActionId", is(88L)),
        hasField("states", contains("state")),
        hasField("statuses", contains(WorkflowInstanceStatus.created)),
        hasField("businessKey", equalTo("businessKey")),
        hasField("externalId", equalTo("externalId")),
        hasField("stateVariableKey", nullValue()),
        hasField("stateVariableValue", nullValue()),
        hasField("includeActions", equalTo(false)),
        hasField("includeCurrentStateVariables", equalTo(false)),
        hasField("includeActionStateVariables", equalTo(false)),
        hasField("includeChildWorkflows", equalTo(false)),
        hasField("maxResults", equalTo(null)),
        hasField("maxActions", equalTo(null)))));
  }

  @Test
  public void listWorkflowInstancesWorksWithAllIncludes() {
    makeRequest(() -> resource.listWorkflowInstances(asList(42L), asList("type"), 99L, 88L, asList("state"),
        asList(WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing), "businessKey", "externalId",
        "stateVarKey", "stateVarValue", "actions,currentStateVariables,actionStateVariables,childWorkflows", 1L, 1L));
    verify(workflowInstances).listWorkflowInstancesAsStream(
        (QueryWorkflowInstances) argThat(allOf(
            hasField("ids", contains(42L)),
            hasField("types", contains("type")),
            hasField("parentWorkflowId", is(99L)),
            hasField("parentActionId", is(88L)),
            hasField("states", contains("state")),
            hasField("statuses", contains(WorkflowInstanceStatus.created, WorkflowInstanceStatus.executing)),
            hasField("businessKey", equalTo("businessKey")),
            hasField("externalId", equalTo("externalId")),
            hasField("stateVariableKey", equalTo("stateVarKey")),
            hasField("stateVariableValue", equalTo("stateVarValue")),
            hasField("includeActions", equalTo(true)),
            hasField("includeCurrentStateVariables", equalTo(true)),
            hasField("includeActionStateVariables", equalTo(true)),
            hasField("includeChildWorkflows", equalTo(true)),
            hasField("maxResults", equalTo(1L)),
            hasField("maxActions", equalTo(1L)))));
  }

  @Test
  public void fetchingNonExistingWorkflowReturnsNotFound() {
    when(workflowInstances.getWorkflowInstance(42, emptySet(), null))
    .thenThrow(new NflowNotFoundException("Workflow instance", 42, new Exception()));
    try (Response response = resource.fetchWorkflowInstance(42, null, null)) {
      assertThat(response.getStatus(), is(equalTo(NOT_FOUND.getStatusCode())));
      assertThat(response.readEntity(ErrorResponse.class).error, is(equalTo("Workflow instance 42 not found")));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fetchingExistingWorkflowWorks() {
    WorkflowInstance instance = mock(WorkflowInstance.class);
    when(workflowInstances.getWorkflowInstance(42, emptySet(), null)).thenReturn(instance);
    ListWorkflowInstanceResponse resp = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance), any(Set.class))).thenReturn(resp);
    ListWorkflowInstanceResponse result = getEntity(() -> resource.fetchWorkflowInstance(42, null, null),
        ListWorkflowInstanceResponse.class);
    verify(workflowInstances).getWorkflowInstance(42, emptySet(), null);
    assertEquals(resp, result);
  }

  @SuppressWarnings({ "unchecked" })
  @Test
  public void fetchingExistingWorkflowWorksWithAllIncludes() {
    WorkflowInstance instance = mock(WorkflowInstance.class);
    EnumSet<WorkflowInstanceInclude> includes = EnumSet.allOf(WorkflowInstanceInclude.class);
    when(workflowInstances.getWorkflowInstance(42, includes, 10L)).thenReturn(instance);
    ListWorkflowInstanceResponse resp = mock(ListWorkflowInstanceResponse.class);
    when(listWorkflowConverter.convert(eq(instance), any(Set.class))).thenReturn(resp);
    ListWorkflowInstanceResponse result = getEntity(
        () -> resource.fetchWorkflowInstance(42, "actions,currentStateVariables,actionStateVariables,childWorkflows", 10L),
        ListWorkflowInstanceResponse.class);
    verify(workflowInstances).getWorkflowInstance(42, includes, 10L);
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

  public static class FieldMatcher<T> extends TypeSafeDiagnosingMatcher<T> {
    private final String fieldName;
    private final Matcher<?> valueMatcher;

    public FieldMatcher(String fieldName, Matcher<?> valueMatcher) {
      this.fieldName = fieldName;
      this.valueMatcher = valueMatcher;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("has field \"").appendText(fieldName).appendText("\"");
      description.appendText(" with value ").appendDescriptionOf(valueMatcher);
    }

    @Override
    protected boolean matchesSafely(T item, Description mismatchDescription) {
      try {
        Field field = item.getClass().getField(fieldName);
        return valueMatcher.matches(field.get(item));
      } catch (NoSuchFieldException | IllegalAccessException e) {
        return false;
      }
    }

    public static <T> FieldMatcher<T> hasField(String fieldName, Matcher<?> withValue) {
      return new FieldMatcher<T>(fieldName, withValue);
    }
  }
}
