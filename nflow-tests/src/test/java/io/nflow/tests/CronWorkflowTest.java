package io.nflow.tests;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.TestCronWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension.BeforeServerStop;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.failed;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.tests.demo.workflow.TestCronWorkflow.TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CronWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(CronConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public CronWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = TestCronWorkflow.class)
  static class CronConfiguration {
    // for component scanning only
  }

  @Test
  @Order(1)
  public void startCronWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = TYPE;
    req.stateVariables = singletonMap("cron", "*/3 * * * * *");
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void letItRunFor5Seconds() throws InterruptedException {
    SECONDS.sleep(5);
  }

  @Test
  @Order(3)
  public void verifyThatIsHasRunPeriodically() {
    int i = 1;
    assertWorkflowInstance(resp.id, actionHistoryValidator(asList(
            new Action(i++, stateExecution.name(), "schedule", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "doWork", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "schedule", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "doWork", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "schedule", "", 0, null, null, 0)
    )));
  }

  @BeforeServerStop
  public void stopMaintenanceWorkflow() {
    UpdateWorkflowInstanceRequest request = new UpdateWorkflowInstanceRequest();
    request.nextActivationTime = null;
    request.state = failed.name();
    updateWorkflowInstance(resp.id, request);
  }
}
