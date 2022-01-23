package io.nflow.tests;

import static io.nflow.tests.demo.workflow.FibonacciWorkflow.FIBONACCI_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.VAR_REQUEST_DATA;
import static java.time.Duration.ofSeconds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
import io.nflow.tests.demo.workflow.TestState;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChildWorkflowTest extends AbstractNflowTest {
  public static NflowServerConfig server = new NflowServerConfig.Builder().build();

  private static long workflowId;

  public ChildWorkflowTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void startFibonacciWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FIBONACCI_TYPE;
    req.stateVariables.put(VAR_REQUEST_DATA, nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(5)));
    CreateWorkflowInstanceResponse resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;
  }

  @Test
  @Order(2)
  public void checkFibonacciWorkflowComputesCorrectResult() {
    ListWorkflowInstanceResponse response = getWorkflowInstanceWithTimeout(workflowId, TestState.DONE.name(), ofSeconds(30));
    assertThat(response.stateVariables, hasEntry("result", 8));
  }
}
