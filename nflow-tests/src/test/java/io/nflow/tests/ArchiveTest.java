package io.nflow.tests;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.time.Duration.ofSeconds;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.engine.service.ArchiveService;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArchiveTest extends AbstractNflowTest {
  private static final int STEP_1_WORKFLOWS = 4;
  private static final int STEP_2_WORKFLOWS = 7;
  private static final int STEP_3_WORKFLOWS = 4;
  private static final int ARCHIVE_TIMEOUT = 15;

  public static NflowServerConfig server = new NflowServerConfig.Builder().prop("nflow.dispatcher.sleep.ms", 25)
      .springContextClass(ArchiveConfiguration.class).build();
  static ArchiveService archiveService;

  private static DateTime archiveLimit1, archiveLimit2;

  public ArchiveTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void cleanupExistingArchivableStuff() {
    assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(DateTime.now(), 10));
  }

  @Test
  @Order(2)
  public void createWorkflows() throws InterruptedException {
    waitUntilWorkflowsFinished(createWorkflows(STEP_1_WORKFLOWS));
    archiveLimit1 = DateTime.now();
    // Make sure first batch of workflows is created before the second batch.
    // (some databases have 1 second precision in timestamps (e.g. mysql 5.5))
    sleep(SECONDS.toMillis(1));
  }

  @Test
  @Order(3)
  public void createMoreWorkflows() throws InterruptedException {
    waitUntilWorkflowsFinished(createWorkflows(STEP_2_WORKFLOWS));
    archiveLimit2 = DateTime.now();
    sleep(SECONDS.toMillis(1));
  }

  @Test
  @Order(4)
  public void archiveBeforeTime1ArchiveAllWorkflows() {
    int archived = assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(archiveLimit1, 3));
    // fibonacci(3) workflow creates 1 child workflow
    assertEquals(STEP_1_WORKFLOWS * 2, archived);
  }

  @Test
  @Order(5)
  public void archiveAgainBeforeTime1DoesNotArchivesAnything() {
    int archived = assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(archiveLimit1, 3));
    assertEquals(0, archived);
  }

  @Test
  @Order(6)
  public void archiveBeforeTime2Archives() {
    int archived = assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(archiveLimit2, 5));
    assertEquals(STEP_2_WORKFLOWS * 2, archived);
  }

  @Test
  @Order(7)
  public void createMoreWorkflows_again() {
    waitUntilWorkflowsFinished(createWorkflows(STEP_3_WORKFLOWS));
  }

  @Test
  @Order(8)
  public void archiveAgainBeforeTime1DoesNotArchiveAnything() {
    int archived = assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(archiveLimit1, 3));
    assertEquals(0, archived);
  }

  @Test
  @Order(9)
  public void archiveAgainBeforeTime2DoesNotArchiveAnything() {
    int archived = assertTimeoutPreemptively(ofSeconds(ARCHIVE_TIMEOUT), () ->
      archiveService.archiveWorkflows(archiveLimit2, 3));
    assertEquals(0, archived);
  }

  private List<Integer> createWorkflows(int count) {
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ids.add(createWorkflow());
    }
    return ids;
  }

  private int createWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FibonacciWorkflow.WORKFLOW_TYPE;
    req.stateVariables.put("requestData", nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(3)));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    return resp.id;
  }

  private void waitUntilWorkflowsFinished(List<Integer> workflowIds) {
    assertTimeoutPreemptively(ofSeconds(15), () -> {
      for (int workflowId : workflowIds) {
         try {
          getWorkflowInstance(workflowId, "done");
        } catch (@SuppressWarnings("unused") InterruptedException e) {
          // ignore
        }
      }
    });
  }

  // TODO another way would be to modify JettyServerContainer to have reference to Spring's applicationContext
  // that would allow accessing ArchiveService via NflowServerRule
  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  private static class ArchiveConfiguration {
    @Inject
    private ArchiveService service;

    @PostConstruct
    public void linkArchiveServiceToTestClass() {
      archiveService = service;
    }

    @PreDestroy
    public void removeArchiveServiceFromTestClass() {
      archiveService = null;
    }
  }
}
