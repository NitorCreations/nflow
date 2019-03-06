package io.nflow.tests;

import static io.nflow.engine.workflow.definition.BulkWorkflow.BULK_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.definition.BulkWorkflow.State.done;
import static io.nflow.tests.demo.workflow.DemoBulkWorkflow.DEMO_BULK_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.util.Arrays.asList;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.util.List;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.engine.workflow.definition.BulkWorkflow;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.DemoBulkWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class BulkWorkflowTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(Configuration.class).build();

  private static int workflowId;

  private static int childrenCount;

  public BulkWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoBulkWorkflow.class)
  static class Configuration {
    // for component scanning only
  }

  @Test
  public void t01_startDemoBulkWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_BULK_WORKFLOW_TYPE;
    req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 3);
    List<Integer> childData = asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    childrenCount = childData.size();
    req.stateVariables.put(BulkWorkflow.VAR_CHILD_DATA, nflowObjectMapper().valueToTree(childData));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;
  }

  @Test(timeout = 30000)
  public void t02_waitForBulkToFinish() throws InterruptedException {
    waitForBulkToFinish();
  }

  @Test
  public void t11_createBulkWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = BULK_WORKFLOW_TYPE;
    req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 3);
    req.activate = false;
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;

    for (int i = 0; i < childrenCount; ++i) {
      CreateWorkflowInstanceRequest child = new CreateWorkflowInstanceRequest();
      child.type = DEMO_WORKFLOW_TYPE;
      child.activate = false;
      child.parentWorkflowId = workflowId;
      resp = fromClient(workflowInstanceResource, true).put(child, CreateWorkflowInstanceResponse.class);
      assertThat(resp.id, notNullValue());
    }
  }

  @Test
  public void t12_startBulkWorkflow() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now();
    try (Response resp = fromClient(workflowInstanceResource, true).path("id").path(workflowId).put(req, Response.class)) {
      assertThat(resp.getStatus(), equalTo(204));
    }
  }

  @Test(timeout = 30000)
  public void t13_waitForBulkToFinish() throws InterruptedException {
    waitForBulkToFinish();
  }

  private void waitForBulkToFinish() throws InterruptedException {
    ListWorkflowInstanceResponse instance = getWorkflowInstance(workflowId, done.name());
    assertThat(instance.childWorkflows.size(), equalTo(1));
    List<Integer> childWorkflowIds = instance.childWorkflows.values().iterator().next();
    assertThat(childWorkflowIds.size(), equalTo(childrenCount));
    List<ListWorkflowInstanceResponse> children = childWorkflowIds.stream().map(this::getWorkflowInstance).collect(toList());
    DateTime minFinished = children.stream().map(child -> child.modified).min(naturalOrder()).get();
    DateTime maxStarted = children.stream().map(child -> child.started).max(naturalOrder()).get();
    assertThat(minFinished, lessThan(maxStarted));
  }

}
