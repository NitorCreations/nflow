package io.nflow.tests;

import static io.nflow.engine.internal.workflow.MaintenanceWorkflowStarter.MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID;
import static io.nflow.engine.workflow.curated.CronWorkflow.FAILED;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.FIBONACCI_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.VAR_REQUEST_DATA;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.Period.seconds;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension.BeforeServerStop;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaintenanceWorkflowTest extends AbstractNflowTest {
  public static NflowServerConfig server = new NflowServerConfig.Builder() //
      .prop("nflow.maintenance.insertWorkflowIfMissing", true) //
      .prop("nflow.maintenance.initial.cron", "* * * * * *") //
      .prop("nflow.maintenance.initial.delete.olderThan", seconds(1).toString()) //
      .build();

  private static List<Long> ids;
  private static long maintenanceWorkflowId;

  public MaintenanceWorkflowTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void cleanupExistingStuff() {
    deleteAllFinishedWorkflows();
  }

  @Test
  @Order(2)
  public void verifyThatMaintenanceWorkflowIsRunning() throws InterruptedException {
    SECONDS.sleep(1);
    ListWorkflowInstanceResponse[] instances = getInstanceResource() //
        .query("type", MAINTENANCE_WORKFLOW_TYPE) //
        .query("externalId", MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID) //
        .query("include", "currentStateVariables") //
        .get(ListWorkflowInstanceResponse[].class);
    assertThat(asList(instances), hasSize(1));
    assertThat(instances[0].stateVariables, hasEntry("cron", "* * * * * *"));
    maintenanceWorkflowId = instances[0].id;
  }

  @Test
  @Order(3)
  public void createWorkflows() {
    ids = createWorkflows(2);
  }

  @Test
  @Order(4)
  public void waitForCleanup() throws InterruptedException {
    SECONDS.sleep(6);
  }

  @Test
  @Order(5)
  public void verifyThatCleanupOccurred() {
    ids.forEach(id -> assertThrows(NotFoundException.class, () -> getWorkflowInstance(id)));
  }

  @BeforeServerStop
  public void stopMaintenanceWorkflow() {
    UpdateWorkflowInstanceRequest request = new UpdateWorkflowInstanceRequest();
    request.nextActivationTime = null;
    request.state = FAILED.name();
    updateWorkflowInstance(maintenanceWorkflowId, request, String.class);
  }

  private List<Long> createWorkflows(int count) {
    return generate(this::createWorkflow).limit(count).collect(toList());
  }

  private long createWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = FIBONACCI_TYPE;
    req.stateVariables.put(VAR_REQUEST_DATA, nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(3)));
    CreateWorkflowInstanceResponse resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
    return resp.id;
  }
}
