package io.nflow.tests;

import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse.StateStatistics;
import io.nflow.tests.DemoWorkflowTest.DemoConfiguration;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StatisticsTest extends AbstractNflowTest {

  static DateTime FUTURE = now().plusYears(1);

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoConfiguration.class)
    .prop("nflow.executor.timeout.seconds", 1)
    .prop("nflow.executor.keepalive.seconds", 5)
    .prop("nflow.dispatcher.await.termination.seconds", 1)
    .prop("nflow.db.h2.url", "jdbc:h2:mem:statisticstest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
    .build();

  private static CreateWorkflowInstanceResponse resp;

  public StatisticsTest() {
    super(server);
  }

  @AfterEach
  public void cleanup() {
    clearProperty("nflow.autostart");
  }

  @Test
  @Order(1)
  public void submitWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_WORKFLOW_TYPE;
    req.businessKey = "1";
    req.activationTime = FUTURE;
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void queryStatistics() {
    StatisticsResponse statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(0, statistics.queueStatistics.count);
  }

  @Test
  @Order(3)
  public void queryDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    assertThat(statistics.stateStatistics, is(notNullValue()));
    StateStatistics stats = statistics.stateStatistics.get("begin");
    assertThat(stats.created.allInstances, is(1L));
    assertThat(stats.created.queuedInstances, is(0L));
  }

  @Test
  @Order(4)
  public void stopServer() {
    // This does not actually stop the executor threads, because JVM does not exit.
    // Connection pool is closed though, so the workflow instance state cannot be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  @Order(5)
  public void restartServer() throws Exception {
    setProperty("nflow.autostart", "false");
    server.startServer();
  }

  @Test
  @Order(6)
  public void updateNextActivationToPast() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now().minusMinutes(5);
    updateWorkflowInstance(resp.id, req, String.class);
  }

  @Test
  @Order(7)
  public void queryStatistics_again() {
    StatisticsResponse statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(1, statistics.queueStatistics.count);
  }

  @Test
  @Order(8)
  public void queryDefinitionStatistics_again() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    assertThat(statistics.stateStatistics, is(notNullValue()));
    StateStatistics stats = statistics.stateStatistics.get("begin");
    assertThat(stats.created.allInstances, is(1L));
    assertThat(stats.created.queuedInstances, is(1L));
  }
}
