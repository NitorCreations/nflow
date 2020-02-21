package io.nflow.tests;

import static io.nflow.engine.workflow.curated.BulkWorkflow.BULK_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.curated.BulkWorkflow.State.done;
import static io.nflow.tests.demo.workflow.DemoBulkWorkflow.DEMO_BULK_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.time.Duration.ofSeconds;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;

import java.util.List;
import java.util.stream.IntStream;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.engine.workflow.curated.BulkWorkflow;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.DemoBulkWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BulkWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().prop("nflow.dispatcher.sleep.ms", 25)
      .springContextClass(Configuration.class).build();

  private static long workflowId;

  private static final int CHILDREN_COUNT = 10;

  public BulkWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoBulkWorkflow.class)
  static class Configuration {
    // for component scanning only
  }

  @Test
  @Order(1)
  public void t01_startDemoBulkWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_BULK_WORKFLOW_TYPE;
    req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 3);
    List<Integer> childData = IntStream.rangeClosed(1, CHILDREN_COUNT).boxed().collect(toList());
    req.stateVariables.put(BulkWorkflow.VAR_CHILD_DATA, nflowObjectMapper().valueToTree(childData));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;
  }

  @Test
  @Order(2)
  public void t02_waitForBulkToFinish() {
    waitForBulkToFinish();
  }

  @Test
  @Order(3)
  public void t11_createBulkWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = BULK_WORKFLOW_TYPE;
    req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 3);
    req.activate = false;
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;

    for (int i = 0; i < CHILDREN_COUNT; ++i) {
      CreateWorkflowInstanceRequest child = new CreateWorkflowInstanceRequest();
      child.type = DEMO_WORKFLOW_TYPE;
      child.activate = false;
      child.parentWorkflowId = workflowId;
      resp = fromClient(workflowInstanceResource, true).put(child, CreateWorkflowInstanceResponse.class);
      assertThat(resp.id, notNullValue());
    }
  }

  @Test
  @Order(4)
  public void t12_startBulkWorkflow() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now();
    try (Response resp = fromClient(workflowInstanceResource, true).path("id").path(workflowId).put(req, Response.class)) {
      assertThat(resp.getStatus(), equalTo(204));
    }
  }

  @Test
  @Order(5)
  public void t13_waitForBulkToFinish() {
    waitForBulkToFinish();
  }

  private void waitForBulkToFinish() {
    ListWorkflowInstanceResponse instance = getWorkflowInstanceWithTimeout(workflowId, done.name(), ofSeconds(30));
    assertThat(instance.childWorkflows.size(), equalTo(1));
    List<Long> childWorkflowIds = instance.childWorkflows.values().iterator().next();
    assertThat(childWorkflowIds.size(), equalTo(CHILDREN_COUNT));
    List<ListWorkflowInstanceResponse> children = childWorkflowIds.stream().map(this::getWorkflowInstance).collect(toList());
    DateTime minFinished = children.stream().map(child -> child.modified).min(naturalOrder()).get();
    DateTime maxStarted = children.stream().map(child -> child.started).max(naturalOrder()).get();
    assertThat(minFinished, lessThan(maxStarted));
  }

}
