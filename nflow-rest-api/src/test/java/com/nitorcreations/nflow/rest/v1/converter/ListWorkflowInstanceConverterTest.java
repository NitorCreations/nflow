package com.nitorcreations.nflow.rest.v1.converter;

import static com.nitorcreations.Matchers.reflectEquals;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;

@RunWith(MockitoJUnitRunner.class)
public class ListWorkflowInstanceConverterTest {
  @Mock
  private ObjectMapper nflowObjectMapper;
  @InjectMocks
  private final ListWorkflowInstanceConverter converter = new ListWorkflowInstanceConverter();

  @Test
  public void convertWithActionsWorks() throws IOException {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).setExecutorId(999).build();
    Map<String, String> stateVariables = new LinkedHashMap<>();
    stateVariables.put("foo", "1");
    stateVariables.put("bar", "quux");

    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
        .setBusinessKey("businessKey").setParentWorkflowId(942).setParentActionId(842).setExternalId("externalId").setState("cState").setStateText("cState desc")
        .setNextActivation(now()).setActions(asList(a)).setCreated(now().minusMinutes(1)).setCreated(now().minusHours(2))
        .setModified(now().minusHours(1)).setRetries(42).setStateVariables(stateVariables).build();

    JsonNode node1 = mock(JsonNode.class);
    JsonNode nodeQuux = mock(JsonNode.class);
    when(nflowObjectMapper.readTree("1")).thenReturn(node1);
    when(nflowObjectMapper.readTree("quux")).thenReturn(nodeQuux);

    Map<String, Object> expectedStateVariables = new LinkedHashMap<>();
    expectedStateVariables.put("foo", node1);
    expectedStateVariables.put("bar", nodeQuux);

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder()
            .setIncludeActions(true).setIncludeCurrentStateVariables(true).build());

    verify(nflowObjectMapper).readTree("1");
    verify(nflowObjectMapper).readTree("quux");
    assertThat(resp.id, is(i.id));
    assertThat(resp.stateVariables, is(expectedStateVariables));
    assertThat(resp.status, is(i.status.name()));
    assertThat(resp.type, is(i.type));
    assertThat(resp.parentWorkflowId, is(i.parentWorkflowId));
    assertThat(resp.parentActionId, is(i.parentActionId));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.created, is(i.created));
    assertThat(resp.modified, is(i.modified));
    assertThat(resp.started, is(i.started));
    assertThat(resp.retries, is(i.retries));
    assertThat(resp.actions, contains(reflectEquals(new Action(a.id, a.type.name(), a.state, a.stateText, a.retryNo,
        a.executionStart, a.executionEnd, a.executorId))));
  }

  @Test
  public void convertWithActionStateVariablesWorks() throws IOException {
    Map<String, String> stateVariables = new LinkedHashMap<>();
    stateVariables.put("foo", "1");
    stateVariables.put("bar", "quux");

    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState").setStateText("oState desc").
            setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).setExecutorId(999).setUpdatedStateVariables(stateVariables).build();

    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
            .setBusinessKey("businessKey").setParentWorkflowId(942).setParentActionId(842).setExternalId("externalId").setState("cState").setStateText("cState desc")
            .setNextActivation(now()).setActions(asList(a)).setCreated(now().minusMinutes(1)).setCreated(now().minusHours(2))
            .setModified(now().minusHours(1)).setRetries(42).build();

    JsonNode node1 = mock(JsonNode.class);
    JsonNode nodeQuux = mock(JsonNode.class);
    when(nflowObjectMapper.readTree("1")).thenReturn(node1);
    when(nflowObjectMapper.readTree("quux")).thenReturn(nodeQuux);

    Map<String, Object> expectedStateVariables = new LinkedHashMap<>();
    expectedStateVariables.put("foo", node1);
    expectedStateVariables.put("bar", nodeQuux);

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder()
            .setIncludeActions(true).setIncludeActionStateVariables(true).build());

    verify(nflowObjectMapper).readTree("1");
    verify(nflowObjectMapper).readTree("quux");
    assertThat(resp.id, is(i.id));
    assertThat(resp.status, is(i.status.name()));
    assertThat(resp.type, is(i.type));
    assertThat(resp.parentWorkflowId, is(i.parentWorkflowId));
    assertThat(resp.parentActionId, is(i.parentActionId));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.created, is(i.created));
    assertThat(resp.modified, is(i.modified));
    assertThat(resp.started, is(i.started));
    assertThat(resp.retries, is(i.retries));
    assertThat(resp.actions, contains(reflectEquals(new Action(a.id, a.type.name(), a.state, a.stateText, a.retryNo,
            a.executionStart, a.executionEnd, a.executorId, expectedStateVariables))));
  }

  @Test
  public void convertWithoutActionsWorks() {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
        .setBusinessKey("businessKey").setExternalId("externalId").setState("cState").setStateText("cState desc")
        .setNextActivation(now()).setActions(asList(a)).build();

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().build());
    assertThat(resp.id, is(i.id));
    assertThat(resp.status, is(i.status.name()));
    assertThat(resp.type, is(i.type));
    assertThat(resp.parentWorkflowId, nullValue());
    assertThat(resp.parentActionId, nullValue());
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, nullValue());
  }


  @Test
  public void convertWithoutStateVariablesWorks() {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
        .setBusinessKey("businessKey").setExternalId("externalId").setState("cState").setStateText("cState desc")
        .setNextActivation(now()).setActions(Arrays.asList(a))
        .build();

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().build());
    assertThat(resp.id, is(i.id));
    assertThat(resp.stateVariables, is((Map<String, Object>)null));
    assertThat(resp.type, is(i.type));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, nullValue());
  }

  @Test
  public void convertWithEmptyStateVariablesWorks() {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
        .setBusinessKey("businessKey").setExternalId("externalId").setState("cState").setStateText("cState desc")
        .setNextActivation(now()).setActions(Arrays.asList(a))
        .setStateVariables(new LinkedHashMap<String, String>()). build();

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().build());
    assertThat(resp.id, is(i.id));
    assertThat(resp.status, is(i.status.name()));
    assertThat(resp.stateVariables, is((Map<String, Object>)null));
    assertThat(resp.type, is(i.type));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, nullValue());
  }

  @Test
  public void convertWithMalformedStateVariablesWorks() throws JsonProcessingException, IOException {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setId(929).setType(stateExecution).setState("oState")
        .setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    Map<String, String> stateVariables = new LinkedHashMap<>();
    String value1 = "{\"a\":1";
    String value2 = "quux\"";
    stateVariables.put("foo", value1);
    stateVariables.put("bar", value2);

    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setStatus(inProgress).setType("dummy")
        .setBusinessKey("businessKey").setExternalId("externalId").setState("cState").setStateText("cState desc")
        .setNextActivation(now()).setActions(Arrays.asList(a))
        .setStateVariables(stateVariables). build();

    when(nflowObjectMapper.readTree(value1)).thenThrow(new JsonParseException("bad data", null));
    when(nflowObjectMapper.readTree(value2)).thenThrow(new JsonParseException("bad data", null));

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().setIncludeCurrentStateVariables(true).build());

    verify(nflowObjectMapper).readTree(value1);
    verify(nflowObjectMapper).readTree(value2);
    Map<String, Object> expectedStateVariables = new LinkedHashMap<>();
    expectedStateVariables.put("foo", new TextNode(value1));
    expectedStateVariables.put("bar", new TextNode(value2));

    assertThat(resp.id, is(i.id));
    assertThat(resp.status, is(i.status.name()));
    assertThat(resp.stateVariables, is(expectedStateVariables));
    assertThat(resp.type, is(i.type));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.externalId, is(i.externalId));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, nullValue());
  }

}
