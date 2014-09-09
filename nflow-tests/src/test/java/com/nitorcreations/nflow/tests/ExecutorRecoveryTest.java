package com.nitorcreations.nflow.tests;

import static com.nitorcreations.nflow.tests.demo.SlowWorkflow.WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;

import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.SlowWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;
import com.nitorcreations.nflow.tests.runner.SkipNotPersistedDatabaseRule;

@FixMethodOrder(NAME_ASCENDING)
public class ExecutorRecoveryTest extends AbstractNflowTest {

  @Rule
  public SkipNotPersistedDatabaseRule skipNotRealDatabaseRule = new SkipNotPersistedDatabaseRule();

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder()
    .prop("nflow.executor.timeout.seconds", 1)
    .prop("nflow.executor.keepalive.seconds", 5)
    .prop("nflow.dispatcher.await.termination.seconds", 1)
    .build();

  private static CreateWorkflowInstanceResponse resp;

  public ExecutorRecoveryTest() {
    super(server);
  }

  @Test
  public void t01_submitSlowWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = WORKFLOW_TYPE;
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_checkSlowWorkflowStarted() throws Exception {
    for (int i=0; i<5; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (wf != null && SlowWorkflow.State.process.name().equals(wf.state)) {
        return;
      }
      sleep(1000);
    }
    fail("Workflow did not enter state " + SlowWorkflow.State.process.name());
  }

  @Test
  public void t03_stopServer() {
    // This does not actually stop the executor threads, because JVM does not exit.
    // Connection pool is closed though, so the workflow instance state cannot be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  public void t04_restartServer() throws Exception {
    server.startServer();
  }

  @Test
  public void t05_checkSlowWorkflowFinishes() throws Exception {
    // TODO: expose executor_id of actions through rest api and check that
    // start & process states are executed by different executors
    for (int i=0; i<30; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (wf != null && SlowWorkflow.State.done.name().equals(wf.state)) {
        return;
      }
      sleep(1000);
    }
  }
}
