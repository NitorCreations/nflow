package io.nflow.tests;

import static io.nflow.rest.v1.ApiWorkflowInstanceInclude.actions;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import jakarta.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ErrorResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DemoWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.executor.group", DemoWorkflowTest.class.getSimpleName())
      .springContextClass(DemoConfiguration.class)
      .build();

  private static CreateWorkflowInstanceResponse resp;

  public DemoWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @Test
  @Order(1)
  public void startDemoWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_WORKFLOW_TYPE;
    req.businessKey = "1";
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void queryDemoWorkflowHistory() {
    ListWorkflowInstanceResponse wfr = assertTimeoutPreemptively(ofSeconds(5), () -> {
      ListWorkflowInstanceResponse wf = null;
      do {
        sleep(200);
        ListWorkflowInstanceResponse[] instances = getInstanceResource()
            .query("type", DEMO_WORKFLOW_TYPE)
            .query("includes", actions.name())
            .get(ListWorkflowInstanceResponse[].class);
        assertThat(instances.length, greaterThanOrEqualTo(1));
        for (ListWorkflowInstanceResponse instance : instances) {
          if (instance.id == resp.id && "done".equals(instance.state) && instance.nextActivation == null) {
            wf = instance;
            break;
          }
        }
      } while (wf == null);
      return wf;
    });
    assertThat(wfr.actions.size(), is(2));
  }

  @Test
  @Order(3)
  public void queryDemoWorkflowWithMultipleStatuses() {
    ListWorkflowInstanceResponse[] instances = getInstanceResource().query("type", DEMO_WORKFLOW_TYPE).query("status", "finished")
        .query("status", "manual").get(ListWorkflowInstanceResponse[].class);
    assertThat(instances.length, greaterThanOrEqualTo(1));
  }

  @Test
  @Order(4)
  public void queryWorkflowWithActionsReturnsEmptyActions() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_WORKFLOW_TYPE;
    req.businessKey = "2";
    req.activationTime = DateTime.now().plusSeconds(5);
    resp = createWorkflowInstance(req);

    ListWorkflowInstanceResponse instance = getWorkflowInstance(resp.id);

    assertThat(instance.actions, is(empty()));
  }

  @Test
  @Order(5)
  public void queryWorkflowWithoutActionsReturnsNullActions() {
    ListWorkflowInstanceResponse instance = getInstanceIdResource(resp.id).get(ListWorkflowInstanceResponse.class);

    assertThat(instance.actions, is(nullValue()));
  }

  @Test
  @Order(6)
  public void updateWorkflowReturnsNoContentWhenInstanceIsUpdated() throws InterruptedException {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now().plusDays(1);
    RuntimeException ex = new RuntimeException("Update did not occur fast enough");
    for (int i = 0; i < 3; ++i) {
      try (Response response = updateWorkflowInstance(resp.id, req, Response.class)) {
        if (response.getStatus() != NO_CONTENT.getStatusCode()) {
          continue;
        }
        assertThat(response.readEntity(String.class), is(""));
        return;
      } catch (RuntimeException e) {
        ex = e;
        SECONDS.sleep(1);
      }
    }
    throw ex;
  }

  @Test
  @Order(7)
  public void updateWorkflowReturnsNotFoundWhenInstanceIsNotFound() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now().plusDays(2);

    try (Response response = updateWorkflowInstance(-1, req, Response.class)) {
      assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
      assertThat(response.getMediaType(), is(APPLICATION_JSON_TYPE));
      assertThat(response.readEntity(ErrorResponse.class).error, is("Workflow instance -1 not found"));
    }
  }

}
