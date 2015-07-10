package com.nitorcreations.nflow.tests;

import com.nitorcreations.nflow.engine.service.ArchiveService;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.DemoWorkflow;
import com.nitorcreations.nflow.tests.demo.FibonacciWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;
import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
public class ArchiveTest extends AbstractNflowTest {
  private static final int STEP_1_WORKFLOWS = 10, STEP_2_WORKFLOWS = 15, STEP_3_WORKFLOWS = 4;

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(ArchiveConfiguration.class).build();
  static ArchiveService archiveService;

  private static DateTime archiveLimit1, archiveLimit2;

  public ArchiveTest() {
    super(server);
  }

  @Test(timeout = 5000)
  public void t01_createWorkflows() throws InterruptedException {
    for(int i = 0; i < STEP_1_WORKFLOWS; i ++){
      createWorkflow();
    }
    Thread.sleep(2000);
    archiveLimit1 = DateTime.now();
  }

  @Test(timeout = 5000)
  public void t02_createMoreWorkflows() throws InterruptedException {
    for(int i = 0; i < STEP_2_WORKFLOWS; i ++){
      createWorkflow();
    }
    Thread.sleep(2000);
    archiveLimit2 = DateTime.now();
  }

  @Test(timeout = 5000)
  public void t03_archiveBeforeTime1ArchiveAllWorkflows() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    assertEquals(STEP_1_WORKFLOWS, archived);
  }

  @Test(timeout = 5000)
  public void t04_archiveAgainBeforeTime1DoesNotArchivesAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    assertEquals(0, archived);
  }

  @Test(timeout = 5000)
  public void t05_archiveBeforeTime1Archives() {
    int archived = archiveService.archiveWorkflows(archiveLimit2, 5);
    assertEquals(STEP_2_WORKFLOWS, archived);
  }

  @Test(timeout = 5000)
  public void t06_createMoreWorkflows() throws InterruptedException {
    for(int i = 0; i < STEP_3_WORKFLOWS; i ++){
      createWorkflow();
    }
    Thread.sleep(2000);
  }

  @Test(timeout = 5000)
  public void t07_archiveAgainBeforeTime1DoesNotArchiveAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit1, 3);
    assertEquals(0, archived);
  }

  @Test(timeout = 5000)
  public void t08_archiveAgainBeforeTime2DoesNotArchiveAnything() {
    int archived = archiveService.archiveWorkflows(archiveLimit2, 3);
    assertEquals(0, archived);
  }

  private int createWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FibonacciWorkflow.WORKFLOW_TYPE;
    // FIXME set fibo parameter to 3 after foreign key problems have been fixed in archiving
    req.requestData = nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(1));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    return resp.id;
  }

  // TODO another way would be to modify JettyServerContainer to have reference to Spring's applicationContext
  // that would allow accessing ArchiveService via NflowServerRule
  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  private static class ArchiveConfiguration {
    @Inject
    private ArchiveService archiveService;

    @PostConstruct
    public void linkArchiveServiceToTestClass() {
      ArchiveTest.archiveService = archiveService;
    }

    @PreDestroy
    public void removeArchiveServiceFromTestClass() {
      ArchiveTest.archiveService = null;
    }

  }
}
