package com.nitorcreations.nflow.tests;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import com.nitorcreations.nflow.tests.demo.DemoWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class FutureWorkflowTest extends AbstractNflowTest {
  static DateTime FUTURE = new DateTime(2038, 1, 1, 1, 2, 3, 321);

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(DemoConfiguration.class).build();

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
      if (asList(env.getActiveProfiles()).contains("nflow.db.mysql")) {
        FUTURE = FUTURE.withMillisOfSecond(0);
      }
    }
  }

  @Test
  public void t01_scheduleDemoWorkflowToTomorrow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "1";
    req.activationTime = FUTURE;
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_verifyStatusNotStarted() throws InterruptedException {
    SECONDS.sleep(10);
    verifyWorkflowNotStarted();
  }

  @Test
  public void t03_testNonUpdate() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    updateWorkflowInstance(resp.id, req);
    verifyWorkflowNotStarted();
  }

  private void verifyWorkflowNotStarted() {
    ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
    assertThat(wf.started, nullValue());
    assertThat(wf.state, is(DemoWorkflow.State.begin.name()));
    assertThat(wf.nextActivation.getMillis(), is(FUTURE.getMillis()));
  }

  @Test
  public void t04_scheduleToNow() {
    UpdateWorkflowInstanceRequest req = new UpdateWorkflowInstanceRequest();
    req.nextActivationTime = now();
    updateWorkflowInstance(resp.id, req);
  }

  @Test
  public void t05_verifyWorkflowRuns() throws InterruptedException {
    SECONDS.sleep(10);
    ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
    assertThat(wf.started, notNullValue());
    assertThat(wf.state, not(is(DemoWorkflow.State.begin.name())));
  }
}
