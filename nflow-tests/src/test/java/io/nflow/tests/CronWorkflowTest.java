package io.nflow.tests;

import static io.nflow.engine.workflow.curated.CronWorkflow.DO_WORK;
import static io.nflow.engine.workflow.curated.CronWorkflow.SCHEDULE;
import static io.nflow.engine.workflow.curated.CronWorkflow.WAIT_FOR_WORK_TO_FINISH;
import static io.nflow.tests.demo.workflow.TestCronWorkflow.TYPE;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.TestCronWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension.BeforeServerStop;

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
    resp = createWorkflowInstance(req);
    assertNotNull(resp.id);
  }

  @Test
  @Order(2)
  public void letItRunForTenSeconds() throws InterruptedException {
    SECONDS.sleep(10);
  }

  @Test
  @Order(3)
  public void verifyItHasRunPeriodically() {
    List<Action> actions = getWorkflowInstance(resp.id).actions;
    long scheduleActions = actions.stream().filter(a -> SCHEDULE.name().equals(a.state)).count();
    long waitActions = actions.stream().filter(a -> WAIT_FOR_WORK_TO_FINISH.name().equals(a.state)).count();
    long doWorkActions = actions.stream().filter(a -> DO_WORK.name().equals(a.state)).count();
    assertThat(scheduleActions, is(greaterThanOrEqualTo(1L)));
    assertThat(waitActions, is(greaterThanOrEqualTo(1L)));
    assertThat(doWorkActions, is(greaterThanOrEqualTo(1L)));
  }

  @BeforeServerStop
  public void stopMaintenanceWorkflow() throws InterruptedException {
    stopCronWorkflow(resp.id);
  }
}
