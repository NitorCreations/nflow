package io.nflow.tests;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.nflow.tests.extension.NflowServerConfig;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.DeleteHistoryWorkflow;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeleteHistoryTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DeleteHistoryConfiguration.class)
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
  @Order(1)
  public void createWorkflowInstance() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DeleteHistoryWorkflow.TYPE;
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, is(notNullValue()));
  }

  @Test // (timeout = 5000)
  @Order(2)
  public void getProcessedInstance() throws Exception {
    instance = getWorkflowInstance(resp.id, DeleteHistoryWorkflow.State.done.name());
  }

  @Test
  @Order(3)
  public void checkInstanceHistoryIsDeleted() {
    assertThat(instance.childWorkflows.size(), is(1));
    assertThat(instance.actions.size(), is(2));
    assertThat(instance.actions.get(0).state, is(equalTo("done")));
    assertThat(instance.actions.get(1).state, is(equalTo("begin")));
    assertThat(instance.stateVariables.size(), is(1));
  }

}
