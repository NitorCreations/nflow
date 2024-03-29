package io.nflow.tests;

import static io.nflow.rest.v1.ApiWorkflowInstanceInclude.actionStateVariables;
import static io.nflow.rest.v1.ApiWorkflowInstanceInclude.actions;
import static io.nflow.tests.demo.workflow.ActionStateVariableWorkflow.MAX_STATE_VAR_VALUE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.Bean;

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
    var response = assertTimeoutPreemptively(ofSeconds(5), () -> {
      ListWorkflowInstanceResponse wf;
      do {
        sleep(200);
        wf = getInstanceIdResource(createResponse.id)
            .query("includes", actions.name())
            .query("includes", actionStateVariables.name())
            .query("maxActions", maxActions)
            .get(ListWorkflowInstanceResponse.class);
      } while (wf == null || !"done".equals(wf.state));
      return wf;
    });

    assertThat(response.actions, hasSize(maxActions));
    for (int i = 0; i < maxActions; i++) {
      var action = response.actions.get(i);
      assertThat(action.updatedStateVariables, hasEntry("stateVar", MAX_STATE_VAR_VALUE - i));
    }
  }
}
