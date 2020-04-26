package io.nflow.tests;

import static io.nflow.engine.config.Profiles.MYSQL;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FutureWorkflowTest extends AbstractNflowTest {
  static DateTime FUTURE = new DateTime(2038, 1, 1, 1, 2, 3, 321);

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public FutureWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    @Inject
    public DemoConfiguration(Environment env) {
      // mysql 5.5.x (from travis) and mariadb 10.0 (only when using mysql jdbc driver instead of mariadb jdbc driver) do not
      // support millisecond precision in timestamps
      if (asList(env.getActiveProfiles()).contains(MYSQL)) {
        FUTURE = FUTURE.withMillisOfSecond(0);
      }
    }
  }

  @Test
  @Order(1)
  public void scheduleDemoWorkflowToTomorrow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "1";
    req.activationTime = FUTURE;
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void verifyStatusNotStarted() throws InterruptedException {
    SECONDS.sleep(10);
    verifyWorkflowNotStarted();
  }

  @Test
  @Order(3)
  public void testNonUpdate() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    updateWorkflowInstance(resp.id, req, String.class);
    verifyWorkflowNotStarted();
  }

  private void verifyWorkflowNotStarted() {
    ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
    assertThat(wf.started, nullValue());
    assertThat(wf.state, is(DemoWorkflow.State.begin.name()));
    assertThat(wf.nextActivation.getMillis(), is(FUTURE.getMillis()));
  }

  @Test
  @Order(4)
  public void scheduleToNow() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now();
    updateWorkflowInstance(resp.id, req, String.class);
  }

  @Test
  @Order(5)
  public void verifyWorkflowRuns() throws InterruptedException {
    SECONDS.sleep(10);
    ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
    assertThat(wf.started, notNullValue());
    assertThat(wf.state, not(is(DemoWorkflow.State.begin.name())));
  }
}
