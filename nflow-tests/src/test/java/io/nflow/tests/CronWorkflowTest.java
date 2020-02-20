package io.nflow.tests;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.AbstractNflowTest;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.demo.workflow.TestCronWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.TimeUnit;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static io.nflow.tests.demo.workflow.TestCronWorkflow.TYPE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@ExtendWith(NflowServerExtension.class)
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
}
