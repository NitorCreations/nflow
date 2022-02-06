package io.nflow.tests;

import static io.nflow.tests.demo.workflow.SimpleWorkflow.SIMPLE_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.StateWorkflow.STATEVAR_QUERYTEST;
import static io.nflow.tests.demo.workflow.StateWorkflow.STATE_WORKFLOW_TYPE;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.workflow.curated.CronWorkflow;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ErrorResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.StateWorkflow;
import io.nflow.tests.demo.workflow.TestState;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StateVariablesTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.workflow.state.variable.value.length", "8000")
      .prop("nflow.executor.group", StateVariablesTest.class.getSimpleName())
      .springContextClass(TestConfiguration.class)
      .build();
  private static CreateWorkflowInstanceRequest createRequest;
  private static CreateWorkflowInstanceResponse createResponse;

  public StateVariablesTest() {
    super(server);
  }

  static class TestConfiguration {
    @Bean
    public StateWorkflow stateWorkflow() {
      return new StateWorkflow();
    }
  }

  @Test
  @Order(1)
  public void createStateWorkflow() throws JsonProcessingException, IOException {
    createRequest = new CreateWorkflowInstanceRequest();
    createRequest.type = STATE_WORKFLOW_TYPE;
    createRequest.externalId = randomUUID().toString();
    createRequest.stateVariables.put("requestData", new ObjectMapper().readTree("{\"test\":5}"));
    createResponse = assertTimeoutPreemptively(ofSeconds(10), () -> createWorkflowInstance(createRequest));
    assertThat(createResponse.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkStateVariables() {
    ListWorkflowInstanceResponse listResponse = getWorkflowInstanceWithTimeout(createResponse.id, "done", ofSeconds(5));

    assertEquals(4, listResponse.stateVariables.size());
    assertEquals(singletonMap("test", 5), listResponse.stateVariables.get("requestData"));
    assertEquals(singletonMap("value", "foo1"), listResponse.stateVariables.get("variable1"));
    assertEquals(singletonMap("value", "bar3"), listResponse.stateVariables.get("variable2"));

    assertEquals(6, listResponse.actions.size());
    assertState(listResponse.actions, 5, StateWorkflow.STATE_1, "foo1", null);
    assertState(listResponse.actions, 4, StateWorkflow.STATE_2, null, "bar1");
    assertState(listResponse.actions, 3, StateWorkflow.STATE_3, null, "bar2");
    assertState(listResponse.actions, 2, StateWorkflow.STATE_4, null, null);
    assertState(listResponse.actions, 1, StateWorkflow.STATE_5, null, "bar3");
    assertState(listResponse.actions, 0, TestState.DONE, null, null);
  }

  @Test
  @Order(3)
  public void updateStateVariable() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.stateVariables.put("testUpdate", "testValue");

    updateWorkflowInstance(createResponse.id, req, String.class);

    ListWorkflowInstanceResponse response = getWorkflowInstance(createResponse.id);
    assertEquals(7, response.actions.size());
    assertThat(response.actions.get(0).updatedStateVariables.get("testUpdate"), is("testValue"));
  }

  @Test
  @Order(4)
  public void updateWorkflowWithTooLongStateVariableValueReturnsBadRequest() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.stateVariables.put("testUpdate", repeat('a', 8001));

    try (Response response = updateWorkflowInstance(createResponse.id, req, Response.class)) {
      assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
      assertThat(response.getMediaType(), is(APPLICATION_JSON_TYPE));
      assertThat(response.readEntity(ErrorResponse.class).error, startsWith("Too long value"));
    }
  }

  @Test
  @Order(5)
  public void insertWorkflowWithTooLongStateVariableValueReturnsBadRequest() {
    createRequest = new CreateWorkflowInstanceRequest();
    createRequest.type = STATE_WORKFLOW_TYPE;
    createRequest.externalId = randomUUID().toString();
    createRequest.stateVariables.put("requestData", repeat('a', 8001));

    try (Response response = getInstanceResource().put(createRequest)) {
      assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
      assertThat(response.getMediaType(), is(APPLICATION_JSON_TYPE));
      assertThat(response.readEntity(ErrorResponse.class).error, startsWith("Too long value"));
    }
  }

  @Test
  @Order(6)
  public void queryWorkflowInstancesDoesNotFindInstanceWithOldStateVariableValue() {
    ListWorkflowInstanceResponse[] instances = getInstanceResource()
        .query("stateVariableKey", STATEVAR_QUERYTEST)
        .query("stateVariableValue", "oldValue")
        .get(ListWorkflowInstanceResponse[].class);

    assertThat(instances.length, is(0));
  }

  @Test
  @Order(7)
  public void queryWorkflowInstancesFindsInstanceWithCurrentStateVariableValue() {
    ListWorkflowInstanceResponse[] instances = getInstanceResource()
        .query("stateVariableKey", STATEVAR_QUERYTEST)
        .query("stateVariableValue", "newValue")
        .get(ListWorkflowInstanceResponse[].class);

    assertThat(instances.length, is(1));
  }

  @Test
  @Order(8)
  public void insertCronStateVariable() {
    createRequest = new CreateWorkflowInstanceRequest();
    createRequest.type = SIMPLE_WORKFLOW_TYPE;
    createRequest.externalId = randomUUID().toString();
    String cronSchedule = "0 0 * * * *";
    createRequest.stateVariables.put(CronWorkflow.VAR_SCHEDULE, cronSchedule);

    createResponse = assertTimeoutPreemptively(ofSeconds(5), () -> createWorkflowInstance(createRequest));
    assertThat(createResponse.id, notNullValue());

    ListWorkflowInstanceResponse listResponse = getWorkflowInstanceWithTimeout(createResponse.id, TestState.DONE.name(),
        ofSeconds(5));
    assertThat(listResponse.stateVariables.size(), is(1));
    assertThat(listResponse.stateVariables.get(CronWorkflow.VAR_SCHEDULE), is(cronSchedule));
  }

  private void assertState(List<Action> actions, int index, WorkflowState state, String variable1, String variable2) {
    Action action = actions.get(index);
    assertEquals(state.name(), action.state);
    assertVariable(action, "variable1", variable1);
    assertVariable(action, "variable2", variable2);
  }

  @SuppressWarnings("unchecked")
  private void assertVariable(Action action, String key, String expectedValue) {
    Map<String, String> value = null;
    if (action.updatedStateVariables != null) {
      value = (Map<String, String>) action.updatedStateVariables.get(key);
    }
    if (value == null) {
      assertNull(expectedValue);
    } else {
      assertEquals(expectedValue, value.get("value"));
    }
  }
}
