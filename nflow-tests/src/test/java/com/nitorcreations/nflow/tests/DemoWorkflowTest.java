package com.nitorcreations.nflow.tests;

import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.DemoWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

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

  public void t02_queryStatistics() {
    Statistics statistics = getStatistics();
    assertThat(statistics.executionStatistics.count + statistics.queuedStatistics.count, greaterThan(0));
  }

  @Test(timeout = 5000)
  public void t03_queryDemoWorkflowHistory() throws Exception {
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

  public void t04_queryStatistics() {
    Statistics statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(0, statistics.queuedStatistics.count);
  }

}
