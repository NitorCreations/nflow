package io.nflow.tests;

import static io.nflow.tests.demo.workflow.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.PROCESS;
import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutorRecoveryTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.executor.timeout.seconds", 1)
      .prop("nflow.executor.keepalive.seconds", 5)
      .prop("nflow.dispatcher.await.termination.seconds", 1)
      .prop("nflow.db.h2.url", "jdbc:h2:mem:executorrecoverytest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
      .build();

  private static CreateWorkflowInstanceResponse resp;

  public ExecutorRecoveryTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void submitSlowWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SLOW_WORKFLOW_TYPE;
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkSlowWorkflowStarted() throws Exception {
    for (int i = 0; i < 5; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (wf != null && PROCESS.name().equals(wf.state)) {
        return;
      }
      sleep(1000);
    }
    fail("Workflow did not enter state " + PROCESS.name());
  }

  @Test
  @Order(3)
  public void stopServer() {
    // This does not actually stop the executor threads, because JVM does not exit.
    // Connection pool is closed though, so the workflow instance state cannot be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  @Order(4)
  public void restartServer() throws Exception {
    server.startServer();
  }

  @Test
  @Order(5)
  public void checkSlowWorkflowFinishes() throws Exception {
    for (int i = 0; i < 30; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (DONE.name().equals(wf.state)) {
        verifyBeginAndProcessAreExecutedByDifferentExecutors(wf);
        return;
      }
      sleep(1000);
    }
    fail("Workflow instance was not recovered");
  }

  private void verifyBeginAndProcessAreExecutedByDifferentExecutors(ListWorkflowInstanceResponse wf) {
    int beginExecutor = 0;
    int processExecutor = 0;
    for (Action action : wf.actions) {
      if ("begin".equals(action.state)) {
        beginExecutor = action.executorId;
      }
      if ("process".equals(action.state)) {
        processExecutor = action.executorId;
      }
    }
    assertThat(beginExecutor, is(not(0)));
    assertThat(processExecutor, is(not(0)));
    assertThat(beginExecutor, is(not(processExecutor)));
  }
}
