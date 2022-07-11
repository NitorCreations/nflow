package io.nflow.tests;

import io.nflow.engine.workflow.curated.MaintenanceWorkflow;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
import io.nflow.tests.demo.workflow.TestState;
import io.nflow.tests.extension.NflowServerConfig;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.BusinessKeyWorkflow.BUSINESS_KEY_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.FIBONACCI_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.VAR_REQUEST_DATA;
import static java.time.Duration.ofSeconds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.joda.time.Period.seconds;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConcurrentExecutorsTest extends AbstractNflowTest {
  public static NflowServerConfig server1 = new NflowServerConfig.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .prop("nflow.dispatcher.executor.queue.size", 2)
          .prop("nflow.executor.thread.count", 2)
          .prop("nflow.dispatcher.sleep.ms", 50)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:concurrent;TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1")
          .prop("nflow.maintenance.insertWorkflowIfMissing", false)
          .build();

  public static NflowServerConfig server2 = new NflowServerConfig.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .prop("nflow.dispatcher.executor.queue.size", 2)
          .prop("nflow.executor.thread.count", 2)
          .prop("nflow.dispatcher.sleep.ms", 50)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:concurrent;TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1")
          .prop("nflow.maintenance.insertWorkflowIfMissing", false)
          .build();

  public static NflowServerConfig server3 = new NflowServerConfig.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .prop("nflow.dispatcher.executor.queue.size", 2)
          .prop("nflow.executor.thread.count", 2)
          .prop("nflow.dispatcher.sleep.ms", 50)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:concurrent;TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1")
          .prop("nflow.maintenance.insertWorkflowIfMissing", false)
          .build();

  private static final List<Long> workflowIds = new ArrayList<>();

  public ConcurrentExecutorsTest() {
    super(server1);
  }

  @Test
  @Order(1)
  public void startWorkflows() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FIBONACCI_TYPE;
    req.stateVariables.put(VAR_REQUEST_DATA, nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(5)));

    for (int i=0; i<50; ++i) {
      CreateWorkflowInstanceResponse resp = createWorkflowInstance(req);
      assertThat(resp.id, notNullValue());
      workflowIds.add(resp.id);
    }
  }

  @Test
  @Order(2)
  public void waitForWorkflowsToFinish() {
    workflowIds.forEach(workflowId -> {
      ListWorkflowInstanceResponse response = getWorkflowInstanceWithTimeout(workflowId, TestState.DONE.name(), ofSeconds(60));
      assertThat(response.stateVariables, hasEntry("result", 8));
    });
  }
}
