package io.nflow.tests;

import static io.nflow.tests.demo.workflow.BulkWorkflow.State.done;
import static io.nflow.tests.demo.workflow.DemoBulkWorkflow.DEMO_BULK_WORKFLOW_TYPE;
import static java.util.Arrays.asList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.BulkWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class BulkWorkflowTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(Configuration.class).build();

  private static int workflowId;

  public BulkWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = BulkWorkflow.class)
  static class Configuration {
    // for component scanning only
  }

  @Test
  public void t01_startBulkWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_BULK_WORKFLOW_TYPE;
    req.stateVariables.put(BulkWorkflow.VAR_CONCURRENCY, 3);
    req.stateVariables.put(BulkWorkflow.VAR_CHILD_DATA, nflowObjectMapper().valueToTree(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
    CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
        CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
    workflowId = resp.id;
  }

  @Test(timeout = 30000)
  public void t02_waitForBulkToFinish() throws InterruptedException {
    ListWorkflowInstanceResponse instance = getWorkflowInstance(workflowId, done.name());
    assertThat(instance.childWorkflows.size(), equalTo(1));
    assertThat(instance.childWorkflows.get(1).size(), equalTo(10));
  }

}
