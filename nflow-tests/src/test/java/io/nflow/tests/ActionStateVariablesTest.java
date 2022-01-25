package io.nflow.tests;

import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.Bean;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.ActionStateVariableWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ActionStateVariablesTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(TestConfiguration.class).build();
  private static CreateWorkflowInstanceRequest createRequest;
  private static CreateWorkflowInstanceResponse createResponse;

  public ActionStateVariablesTest() {
    super(server);
  }

  static class TestConfiguration {
    @Bean
    public ActionStateVariableWorkflow actionStateVariableWorkflow() {
      return new ActionStateVariableWorkflow();
    }
  }

  @Test
  @Order(1)
  public void createWorkflow() {
    createRequest = new CreateWorkflowInstanceRequest();
    createRequest.type = ActionStateVariableWorkflow.WORKFLOW_TYPE;
    createRequest.externalId = randomUUID().toString();
    createResponse = assertTimeoutPreemptively(ofSeconds(5), () -> createWorkflowInstance(createRequest));
    assertThat(createResponse.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkActionStateVariables() {
    int maxActions = 5;
    ListWorkflowInstanceResponse response = assertTimeoutPreemptively(ofSeconds(5), () -> {
      ListWorkflowInstanceResponse wf = null;
      do {
        sleep(200);
        wf = getInstanceIdResource(createResponse.id)
            .query("include", "actions,actionStateVariables")
            .query("maxActions", maxActions)
            .get(ListWorkflowInstanceResponse.class);
      } while (wf == null || !"done".equals(wf.state));
      return wf;
    });

    assertThat(response.actions, hasSize(maxActions));
    for (int i = 0; i < maxActions; i++) {
      Action action = response.actions.get(i);
      assertEquals(ActionStateVariableWorkflow.MAX_STATE_VAR_VALUE - i, action.updatedStateVariables.get("stateVar"));
    }
  }
}
