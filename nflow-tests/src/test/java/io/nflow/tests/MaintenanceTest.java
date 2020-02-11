package io.nflow.tests;

import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceRequest.MaintenanceRequestItem;
import io.nflow.rest.v1.msg.MaintenanceResponse;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaintenanceTest extends AbstractNflowTest {
  private static final int STEP_1_WORKFLOWS = 4;
  private static final int STEP_2_WORKFLOWS = 7;
  private static final int STEP_3_WORKFLOWS = 4;
  private static final Duration ARCHIVE_TIMEOUT = ofSeconds(15);

  public static NflowServerConfig server = new NflowServerConfig.Builder().prop("nflow.dispatcher.sleep.ms", 25).build();

  private static DateTime timeLimit1, timeLimit2;

  public MaintenanceTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void cleanupExistingArchivableStuff() {
    archiveOlderThan(now());
  }

  @Test
  @Order(2)
  public void createWorkflows() throws InterruptedException {
    waitUntilWorkflowsFinished(createWorkflows(STEP_1_WORKFLOWS));
    timeLimit1 = now();
    // Sleep to make sure that first batch of workflows is created before the second batch.
    // Some databases have 1 second precision in timestamps, for example MySQL 5.5.
    sleep(SECONDS.toMillis(1));
  }

  @Test
  @Order(3)
  public void createMoreWorkflows() throws InterruptedException {
    waitUntilWorkflowsFinished(createWorkflows(STEP_2_WORKFLOWS));
    timeLimit2 = now();
    sleep(SECONDS.toMillis(1));
  }

  @Test
  @Order(4)
  public void archiveBeforeTimeLimit1Archives() {
    int archived = archiveOlderThan(timeLimit1);
    // fibonacci(3) workflow creates 1 child workflow
    assertEquals(STEP_1_WORKFLOWS * 2, archived);
  }

  @Test
  @Order(5)
  public void archiveAgainBeforeTimeLimit1DoesNothing() {
    int archived = archiveOlderThan(timeLimit1);
    assertEquals(0, archived);
  }

  @Test
  @Order(6)
  public void archiveBeforeTimeLimit2Archives() {
    int archived = archiveOlderThan(timeLimit2);
    assertEquals(STEP_2_WORKFLOWS * 2, archived);
  }

  @Test
  @Order(7)
  public void createMoreWorkflowsAgain() {
    waitUntilWorkflowsFinished(createWorkflows(STEP_3_WORKFLOWS));
  }

  @Test
  @Order(8)
  public void archiveOnceMoreBeforeTimeLimit1DoesNothing() {
    int archived = archiveOlderThan(timeLimit1);
    assertEquals(0, archived);
  }

  @Test
  @Order(9)
  public void archiveAgainBeforeTimeLimit2DoesNothing() {
    int archived = archiveOlderThan(timeLimit2);
    assertEquals(0, archived);
  }

  @Test
  @Order(10)
  public void deleteBeforeTimeLimit1Deletes() {
    int deleted = deleteOlderThan(timeLimit1);
    // fibonacci(3) workflow creates 1 child workflow
    assertEquals(STEP_1_WORKFLOWS * 2, deleted);
  }

  @Test
  @Order(11)
  public void deleteAgainBeforeTimeLimit1DoesNothing() {
    int deleted = deleteOlderThan(timeLimit1);
    assertEquals(0, deleted);
  }

  @Test
  @Order(12)
  public void deleteBeforeTimeLimit2Deletes() {
    int deleted = deleteOlderThan(timeLimit2);
    // fibonacci(3) workflow creates 1 child workflow
    assertEquals(STEP_2_WORKFLOWS * 2, deleted);
  }

  @Test
  @Order(13)
  public void deleteAgainBeforeTimeLimit2DoesNothing() {
    int archived = archiveOlderThan(timeLimit2);
    assertEquals(0, archived);
  }

  private List<Long> createWorkflows(int count) {
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ids.add(createWorkflow());
    }
    return ids;
  }

  private long createWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FibonacciWorkflow.WORKFLOW_TYPE;
    req.stateVariables.put("requestData", nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(3)));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    return resp.id;
  }

  private int archiveOlderThan(DateTime olderThan) {
    MaintenanceRequest req = new MaintenanceRequest();
    req.archiveWorkflows = new MaintenanceRequestItem();
    req.archiveWorkflows.olderThanPeriod = new Period(olderThan, now());
    return assertTimeoutPreemptively(ARCHIVE_TIMEOUT,
        () -> fromClient(maintenanceResource).type(APPLICATION_JSON_TYPE).post(req, MaintenanceResponse.class)).archivedWorkflows;
  }

  private int deleteOlderThan(DateTime olderThan) {
    MaintenanceRequest req = new MaintenanceRequest();
    req.deleteArchivedWorkflows = new MaintenanceRequestItem();
    req.deleteArchivedWorkflows.olderThanPeriod = new Period(olderThan, now());
    return assertTimeoutPreemptively(ARCHIVE_TIMEOUT, () -> fromClient(maintenanceResource).type(APPLICATION_JSON_TYPE).post(req,
        MaintenanceResponse.class)).deletedArchivedWorkflows;
  }

  private void waitUntilWorkflowsFinished(List<Long> workflowIds) {
    assertTimeoutPreemptively(ofSeconds(15), () -> {
      for (long workflowId : workflowIds) {
        try {
          getWorkflowInstance(workflowId, "done");
        } catch (@SuppressWarnings("unused") InterruptedException e) {
          // ignore
        }
      }
    });
  }
}
