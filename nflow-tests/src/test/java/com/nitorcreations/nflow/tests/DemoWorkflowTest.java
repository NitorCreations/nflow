package com.nitorcreations.nflow.tests;

import static com.nitorcreations.nflow.tests.demo.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.util.Map;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.DefinitionStatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
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

  @Test
  @Ignore("Fails with MySQL in Travis")
  public void t02_queryStatistics() {
    StatisticsResponse statistics = getStatistics();
    assertThat(statistics.executionStatistics.count + statistics.queueStatistics.count, greaterThan(0));
  }

  @Test
  @Ignore("Does not always work on Travis")
  public void t03_queryDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    Map<String, DefinitionStatisticsResponse> map = statistics.stateStatistics.get("begin");
    DefinitionStatisticsResponse response = map.get("created");
    assertThat(response.allInstances + response.queuedInstances, is(1L));
  }

  @Test(timeout = 5000)
  public void t04_queryDemoWorkflowHistory() throws Exception {
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
  public void t05_queryStatistics() {
    StatisticsResponse statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(0, statistics.queueStatistics.count);
  }

  @Test
  public void t06_queryDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    assertThat(statistics.stateStatistics, is(notNullValue()));
    Map<String, DefinitionStatisticsResponse> map = statistics.stateStatistics.get("done");

    assertThat(map.get("finished").allInstances, is(1L));
    assertThat(map.get("finished").queuedInstances, is(0L));
  }
}
