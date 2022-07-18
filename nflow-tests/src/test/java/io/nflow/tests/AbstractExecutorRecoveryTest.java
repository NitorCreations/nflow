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

import java.util.IdentityHashMap;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractExecutorRecoveryTest extends AbstractNflowTest {

  private final NflowServerConfig server;
  private final Map<String, Object> stateVariables;
  private static final Map<Class<? extends AbstractExecutorRecoveryTest>, CreateWorkflowInstanceResponse> response = new IdentityHashMap<>();
  private final int expectedRecoveryCount;
  private final int expectedFailureCount;

  protected AbstractExecutorRecoveryTest(NflowServerConfig server, Map<String, Object> stateVariables, int expectedRecoveryCount, int expectedFailureCount) {
    super(server);
    this.server = server;
    this.stateVariables = stateVariables;
    this.expectedRecoveryCount = expectedRecoveryCount;
    this.expectedFailureCount = expectedFailureCount;
  }

  @Test
  @Order(1)
  public void submitSlowWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SLOW_WORKFLOW_TYPE;
    req.stateVariables = stateVariables;
    var resp = createWorkflowInstance(req);
    response.put(getClass(), resp);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkSlowWorkflowStarted() throws Exception {
    var resp = response.get(getClass());
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
    var resp = response.get(getClass());
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
    int processStepCount = 0;
    int recoveryCount = 0;
    int failureCount = 0;
    for (Action action : wf.actions) {
      if ("begin".equals(action.state)) {
        beginExecutor = action.executorId;
      }
      if ("process".equals(action.state)) {
        processStepCount++;
        if (processExecutor == 0) {
          processExecutor = action.executorId;
        }
        recoveryCount += "recovery".equals(action.type) ? 1 : 0;
        failureCount += "stateExecutionFailed".equals(action.type) ? 1 : 0;
      }
    }
    assertThat(beginExecutor, is(not(0)));
    assertThat(processExecutor, is(not(0)));
    assertThat(beginExecutor, is(not(processExecutor)));
    assertThat(processStepCount, is(2));
    assertThat(recoveryCount, is(expectedRecoveryCount));
    assertThat(failureCount, is(expectedFailureCount));
  }
}
