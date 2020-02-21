package io.nflow.tests;

import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DemoWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoConfiguration.class).build();

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
    req.type = "demo";
    req.businessKey = "1";
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void queryDemoWorkflowHistory() {
    ListWorkflowInstanceResponse wfr = assertTimeoutPreemptively(ofSeconds(5), () -> {
      ListWorkflowInstanceResponse wf = null;
      do {
        sleep(200);
        ListWorkflowInstanceResponse[] instances = fromClient(workflowInstanceResource, true).query("type", "demo")
            .query("include", "actions").get(ListWorkflowInstanceResponse[].class);
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
    ListWorkflowInstanceResponse[] instances = fromClient(workflowInstanceResource, true).query("type", "demo")
        .query("status", "finished").query("status", "manual").get(ListWorkflowInstanceResponse[].class);
    assertThat(instances.length, greaterThanOrEqualTo(1));
  }

  @Test
  @Order(4)
  public void queryWorkflowWithActionsReturnsEmptyActions() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "2";
    resp = createWorkflowInstance(req);

    ListWorkflowInstanceResponse instance = getWorkflowInstance(resp.id);

    assertThat(instance.actions, is(empty()));
  }

  @Test
  @Order(5)
  public void queryWorkflowWithoutActionsReturnsNullActions() {
    ListWorkflowInstanceResponse instance = fromClient(workflowInstanceIdResource, true).path(Long.toString(resp.id))
        .get(ListWorkflowInstanceResponse.class);

    assertThat(instance.actions, is(nullValue()));
  }

}
