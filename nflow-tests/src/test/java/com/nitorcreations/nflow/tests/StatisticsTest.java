package com.nitorcreations.nflow.tests;

import static com.nitorcreations.nflow.tests.demo.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse.StateStatistics;
import com.nitorcreations.nflow.tests.DemoWorkflowTest.DemoConfiguration;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class StatisticsTest extends AbstractNflowTest {

  static DateTime FUTURE = now().plusYears(1);

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(DemoConfiguration.class)
    .prop("nflow.executor.timeout.seconds", 1)
    .prop("nflow.executor.keepalive.seconds", 5)
    .prop("nflow.dispatcher.await.termination.seconds", 1)
    .prop("nflow.db.h2.url", "jdbc:h2:mem:statisticstest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
    .build();

  private static CreateWorkflowInstanceResponse resp;

  public StatisticsTest() {
    super(server);
  }

  @After
  public void cleanup() {
    clearProperty("nflow.autostart");
  }

  @Test
  public void t01_submitWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_WORKFLOW_TYPE;
    req.businessKey = "1";
    req.activationTime = FUTURE;
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_queryStatistics() {
    StatisticsResponse statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(0, statistics.queueStatistics.count);
  }

  @Test
  public void t03_queryDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    assertThat(statistics.stateStatistics, is(notNullValue()));
    StateStatistics stats = statistics.stateStatistics.get("begin");
    assertThat(stats.created.allInstances, is(1L));
    assertThat(stats.created.queuedInstances, is(0L));
  }

  @Test
  public void t04_stopServer() {
    // This does not actually stop the executor threads, because JVM does not exit.
    // Connection pool is closed though, so the workflow instance state cannot be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  public void t05_restartServer() throws Exception {
    setProperty("nflow.autostart", "false");
    server.startServer();
  }

  @Test
  public void t06_updateNextActivationToPast() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now().minusMinutes(5);
    Response response = fromClient(workflowInstanceResource, true).path(resp.id).put(req);
    assertThat(response.getStatusInfo().getFamily(), is(Family.SUCCESSFUL));
  }

  @Test
  public void t07_queryStatistics() {
    StatisticsResponse statistics = getStatistics();
    assertEquals(0, statistics.executionStatistics.count);
    assertEquals(1, statistics.queueStatistics.count);
  }

  @Test
  public void t08_queryDefinitionStatistics() {
    WorkflowDefinitionStatisticsResponse statistics = getDefinitionStatistics(DEMO_WORKFLOW_TYPE);
    assertThat(statistics.stateStatistics, is(notNullValue()));
    StateStatistics stats = statistics.stateStatistics.get("begin");
    assertThat(stats.created.allInstances, is(1L));
    assertThat(stats.created.queuedInstances, is(1L));
  }
}
