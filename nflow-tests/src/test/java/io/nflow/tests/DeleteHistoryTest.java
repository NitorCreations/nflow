package io.nflow.tests;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
import io.nflow.tests.demo.workflow.DeleteHistoryWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class DeleteHistoryTest extends AbstractNflowTest {
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(DeleteHistoryConfiguration.class)
      .build();

  private static CreateWorkflowInstanceResponse resp;

  private static ListWorkflowInstanceResponse instance;

  public DeleteHistoryTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DeleteHistoryWorkflow.class)
  static class DeleteHistoryConfiguration {
    // for component scanning only
  }

  @Test
  public void t01_createWorkflowInstance() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DeleteHistoryWorkflow.TYPE;
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, is(notNullValue()));
  }

  @Test(timeout = 5000)
  public void t02_getProcessedInstance() throws Exception {
    instance = getWorkflowInstance(resp.id, DeleteHistoryWorkflow.State.done.name());
  }

  @Test
  public void t03_checkInstanceHistoryIsDeleted() {
    assertThat(instance.childWorkflows.size(), is(1));
    assertThat(instance.actions.size(), is(2));
    assertThat(instance.actions.get(0).state, is(equalTo("done")));
    assertThat(instance.actions.get(1).state, is(equalTo("begin")));
    assertThat(instance.stateVariables.size(), is(1));
  }

}
