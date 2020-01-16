package io.nflow.tests;

import io.nflow.engine.workflow.definition.BulkWorkflow;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.tests.demo.workflow.perf.PerfBulkWorkflow;
import io.nflow.tests.demo.workflow.perf.PerfParentWorkflow;
import io.nflow.tests.demo.workflow.perf.PerfPlainWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerfWorkflowTest extends AbstractNflowTest {
  public static NflowServerConfig server = new NflowServerConfig.Builder()
          .prop("nflow.dispatcher.sleep.ms",10)
          .prop("nflow.db.max_pool_size", 8)
          .prop("nflow.executor.fetchChildWorkflowIds", false)
          .springContextClass(PerfWorkflowTest.TestConfiguration.class)
          .build();

  private static final List<Long> workflowIds = new ArrayList<>();

  public PerfWorkflowTest() {
    super(server);
  }

  @ComponentScan("io.nflow.tests.demo.workflow.perf")
  static class TestConfiguration {
  }

  @Test
  @Order(1)
  public void createWorkflows() {
    // against localhost postgresql 12 fsync=off
    // 4 connections:
    // 300 -> 30k workflows. Timings: insert+run  70s (430/s), sql: delete from nflow_worklfow: 130s (230/s)
    // 400 -> 55k workflows. Timings: insert+run 160s (343/s), sql: delete from nflow_worklfow: 410s (134/s)
    // 500 -> 84k workflows, 400k states 370k actions. Timings: insert+run 350s (240/s), sql delete from nflow_worklfow: 1030s (81/s)
    // 8 connections:
    // 1000 -> 336k workflows, 1600k states 1450k actions, sqldump size 247MiB. Timings: insert+run 2980s (112/s)

    for (int i=0; i<300; ++i) {
      CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
      switch (i%3) {
        case 0:
          req.type = PerfBulkWorkflow.TYPE;
          req.stateVariables.put(BulkWorkflow.VAR_CHILD_DATA, "{\"count\":" + i + ",\"type\":\""+(i%2==0 ? PerfPlainWorkflow.TYPE: PerfParentWorkflow.TYPE) + "\"}");
          req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 4);
          break;
        case 1:
          req.type = PerfParentWorkflow.TYPE;
          break;
        case 2:
          req.type = PerfPlainWorkflow.TYPE;
          break;
      }
      CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
              CreateWorkflowInstanceResponse.class);
      assertThat(resp.id, notNullValue());
      workflowIds.add(resp.id);
    }
  }

  @Test
  @Order(2)
  public void waitForCompletion() throws InterruptedException {
    StatisticsResponse stats;
    do {
      SECONDS.sleep(1);
      stats = getStatistics();
      System.out.println("Queue: " + stats.queueStatistics.count);
    } while (stats.queueStatistics.count > 0);
  }
}
