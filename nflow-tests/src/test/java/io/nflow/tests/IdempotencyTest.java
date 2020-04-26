package io.nflow.tests;

import static io.nflow.tests.demo.workflow.SimpleWorkflow.SIMPLE_WORKFLOW_TYPE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.SimpleWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdempotencyTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().prop("nflow.dispatcher.sleep.ms", 25).build();
  private static final CreateWorkflowInstanceRequest request = createWorkflowInstanceRequest();
  private static long firstWorkflowId;
  private static long secondWorkflowId;

  public IdempotencyTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void cleanupExistingStuff() {
    deleteAllFinishedWorkflows();
  }

  @Test
  @Order(2)
  public void createFirstWorkflow() {
    firstWorkflowId = insertWorkflow(request);
  }

  @Test
  @Order(3)
  public void createFirstWorkflowAgainReturnsSameId() {
    long workflowId = insertWorkflow(request);
    assertEquals(firstWorkflowId, workflowId);
  }

  @Test
  @Order(4)
  public void firstWorkflowIsArchived() {
    waitUntilWorkflowIsFinished(firstWorkflowId, SimpleWorkflow.State.done.name());
    int archived = archiveAllFinishedWorkflows().archivedWorkflows;
    assertEquals(1, archived);
  }

  @Test
  @Order(5)
  public void createSameWorkflowAgainsReturnsNewId() {
    secondWorkflowId = insertWorkflow(request);
    assertNotEquals(firstWorkflowId, secondWorkflowId);
  }

  @Test
  @Order(6)
  public void createSecondWorkflowAgainReturnsSameId() {
    long workflowId = insertWorkflow(request);
    assertEquals(secondWorkflowId, workflowId);
  }

  @Test
  @Order(7)
  public void secondWorkflowIsArchived() {
    waitUntilWorkflowIsFinished(secondWorkflowId, SimpleWorkflow.State.done.name());
    int archived = archiveAllFinishedWorkflows().archivedWorkflows;
    assertEquals(1, archived);
  }

  @Test
  @Order(8)
  public void createSameWorkflowAgainsReturnsNewIdAgain() {
    long workflowId = insertWorkflow(request);
    assertNotEquals(secondWorkflowId, workflowId);
  }

  private static CreateWorkflowInstanceRequest createWorkflowInstanceRequest() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SIMPLE_WORKFLOW_TYPE;
    req.externalId = randomUUID().toString();
    return req;
  }
}
