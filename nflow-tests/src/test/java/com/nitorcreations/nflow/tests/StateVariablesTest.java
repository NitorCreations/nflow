package com.nitorcreations.nflow.tests;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.StateWorkflow;
import com.nitorcreations.nflow.tests.demo.StateWorkflow.State;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class StateVariablesTest extends AbstractNflowTest {
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(TestConfiguration.class).build();
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

  @Test(timeout = 5000)
  public void t01_createStateWorkflow() throws JsonProcessingException, IOException {
    createRequest = new CreateWorkflowInstanceRequest();
    createRequest.type = "stateWorkflow";
    createRequest.externalId = UUID.randomUUID().toString();
    createRequest.requestData = new ObjectMapper().readTree("{\"test\":5}");
    createResponse = createWorkflowInstance(createRequest);
    assertThat(createResponse.id, notNullValue());
  }

  @Test(timeout = 5000)
  public void t02_checkStateVariables() throws InterruptedException {
    ListWorkflowInstanceResponse listResponse;
    do {
      listResponse = getWorkflowInstance(createResponse.id, "done");
    } while (listResponse.nextActivation != null);
    assertEquals(3, listResponse.stateVariables.size());
    assertEquals(singletonMap("test", 5), listResponse.stateVariables.get("requestData"));
    assertEquals(singletonMap("value", "foo1"), listResponse.stateVariables.get("variable1"));
    assertEquals(singletonMap("value", "bar3"), listResponse.stateVariables.get("variable2"));

    assertEquals(6, listResponse.actions.size());
    assertState(listResponse.actions, 0, StateWorkflow.State.state1, "foo1", null);
    assertState(listResponse.actions, 1, StateWorkflow.State.state2, null, "bar1");
    assertState(listResponse.actions, 2, StateWorkflow.State.state3, null, "bar2");
    assertState(listResponse.actions, 3, StateWorkflow.State.state4, null, null);
    assertState(listResponse.actions, 4, StateWorkflow.State.state5, null, "bar3");
    assertState(listResponse.actions, 5, StateWorkflow.State.done, null, null);
  }

  private void assertState(List<Action> actions, int index, State state, String variable1, String variable2) {
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

    if (value == null && expectedValue == null) {
      return;
    }
    assertNotNull(value);
    assertEquals(expectedValue, value.get("value"));
  }
}
