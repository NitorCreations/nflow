package com.nitorcreations.nflow.tests;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import com.nitorcreations.nflow.engine.service.ArchiveService;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.DemoWorkflow;
import com.nitorcreations.nflow.tests.demo.FibonacciWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class ArchiveTest extends AbstractNflowTest {
  private static final int STEP_1_WORKFLOWS = 4;
  private static final int STEP_2_WORKFLOWS = 7;
  private static final int STEP_3_WORKFLOWS = 4;
  private static final int CREATE_TIMEOUT = 15000;
  private static final int ARCHIVE_TIMEOUT = 15000;

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().prop("nflow.dispatcher.sleep.ms", 25)
      .springContextClass(ArchiveConfiguration.class).build();
  static ArchiveService archiveService;

  private static DateTime archiveLimit1, archiveLimit2;

  public ArchiveTest() {
    super(server);
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t00_cleanupExistingArchivableStuff() {
    archiveService.archiveWorkflows(DateTime.now(), 10);
  }

  @Test(timeout = CREATE_TIMEOUT)
  public void t01_createWorkflows() {
    waitUntilWorkflowsFinished(createWorkflows(STEP_1_WORKFLOWS));
    archiveLimit1 = DateTime.now();
  }

  @Test(timeout = CREATE_TIMEOUT)
  public void t02_createMoreWorkflows() {
    waitUntilWorkflowsFinished(createWorkflows(STEP_2_WORKFLOWS));
    archiveLimit2 = DateTime.now();
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t03_archiveBeforeTime1ArchiveAllWorkflows() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    // fibonacci(3) workflow creates 1 child workflow
    assertEquals(STEP_1_WORKFLOWS * 2, archived);
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t04_archiveAgainBeforeTime1DoesNotArchivesAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    assertEquals(0, archived);
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t05_archiveBeforeTime2Archives() {
    int archived = archiveService.archiveWorkflows(archiveLimit2, 5);
    assertEquals(STEP_2_WORKFLOWS * 2, archived);
  }

  @Test(timeout = CREATE_TIMEOUT)
  public void t06_createMoreWorkflows() {
    waitUntilWorkflowsFinished(createWorkflows(STEP_3_WORKFLOWS));
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t07_archiveAgainBeforeTime1DoesNotArchiveAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    assertEquals(0, archived);
  }

  @Test(timeout = ARCHIVE_TIMEOUT)
  public void t08_archiveAgainBeforeTime2DoesNotArchiveAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit2, 3);
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
    req.requestData = nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(3));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    return resp.id;
  }

  private void waitUntilWorkflowsFinished(List<Integer> workflowIds) {
    for (int workflowId : workflowIds) {
      try {
        getWorkflowInstance(workflowId, "done");
      } catch (InterruptedException e) {
        // ignore
      }
    }
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
