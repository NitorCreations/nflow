package io.nflow.tests;

import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.DemoWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class DemoWorkflowTest extends AbstractNflowTest {
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(DemoConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public DemoWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @Test
  public void t01_startDemoWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "1";
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test(timeout = 5000)
  public void t02_queryDemoWorkflowHistory() throws Exception {
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
    assertThat(wf.actions.size(), is(2));
  }

  @Test
  public void t03_queryDemoWorkflowWithMultipleStatuses() {
    ListWorkflowInstanceResponse[] instances = fromClient(workflowInstanceResource, true).query("type", "demo")
        .query("status", "finished").query("status", "manual").get(ListWorkflowInstanceResponse[].class);
    assertThat(instances.length, greaterThanOrEqualTo(1));
  }

  @Test
  public void t04_queryWorkflowWithActionsReturnsEmptyActions() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "2";
    resp = createWorkflowInstance(req);

    ListWorkflowInstanceResponse instance = getWorkflowInstance(resp.id);

    assertThat(instance.actions, is(empty()));
  }

  @Test
  public void t05_queryWorkflowWithoutActionsReturnsNullActions() {
    ListWorkflowInstanceResponse instance = fromClient(workflowInstanceResource, true).path(Integer.toString(resp.id))
        .get(ListWorkflowInstanceResponse.class);

    assertThat(instance.actions, is(nullValue()));
  }

}
