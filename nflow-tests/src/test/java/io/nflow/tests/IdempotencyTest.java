package io.nflow.tests;

import static io.nflow.tests.demo.workflow.SimpleWorkflow.SIMPLE_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static java.util.UUID.randomUUID;
import static org.joda.time.Period.seconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceRequest.MaintenanceRequestItem;
import io.nflow.rest.v1.msg.MaintenanceResponse;
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
    firstWorkflowId = createWorkflowInstance(request).id;
  }

  @Test
  @Order(3)
  public void createFirstWorkflowAgainReturnsSameId() {
    long workflowId = createWorkflowInstance(request).id;
    assertEquals(firstWorkflowId, workflowId);
  }

  @Test
  @Order(4)
  public void firstWorkflowIsArchived() {
    waitUntilWorkflowIsFinished(firstWorkflowId, DONE.name());
    int archived = archiveAllFinishedWorkflows().archivedWorkflows;
    assertEquals(1, archived);
  }

  @Test
  @Order(5)
  public void createSameWorkflowAgainsReturnsNewId() {
    secondWorkflowId = createWorkflowInstance(request).id;
    assertNotEquals(firstWorkflowId, secondWorkflowId);
  }

  @Test
  @Order(6)
  public void createSecondWorkflowAgainReturnsSameId() {
    long workflowId = createWorkflowInstance(request).id;
    assertEquals(secondWorkflowId, workflowId);
  }

  @Test
  @Order(7)
  public void secondWorkflowIsArchived() {
    waitUntilWorkflowIsFinished(secondWorkflowId, DONE.name());
    int archived = archiveAllFinishedWorkflows().archivedWorkflows;
    assertEquals(1, archived);
  }

  @Test
  @Order(8)
  public void createSameWorkflowAgainsReturnsNewIdAgain() {
    long workflowId = createWorkflowInstance(request).id;
    assertNotEquals(secondWorkflowId, workflowId);
  }

  private static CreateWorkflowInstanceRequest createWorkflowInstanceRequest() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SIMPLE_WORKFLOW_TYPE;
    req.externalId = randomUUID().toString();
    return req;
  }

  private MaintenanceResponse archiveAllFinishedWorkflows() {
    MaintenanceRequest req = new MaintenanceRequest();
    req.archiveWorkflows = new MaintenanceRequestItem();
    req.archiveWorkflows.olderThanPeriod = seconds(0);
    return doMaintenance(req);
  }
}
