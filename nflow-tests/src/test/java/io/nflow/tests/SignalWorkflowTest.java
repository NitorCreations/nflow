package io.nflow.tests;

import static io.nflow.tests.demo.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.DemoWorkflow;
import io.nflow.tests.demo.SlowWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class SignalWorkflowTest extends AbstractNflowTest {
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(DemoConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public SignalWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @Test
  public void t01_startSlowWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SLOW_WORKFLOW_TYPE;
    req.businessKey = "1";
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_checkSlowWorkflowIsRunning() throws Exception {
    for (int i = 0; i < 10; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (wf != null && SlowWorkflow.State.process.name().equals(wf.state)) {
        return;
      }
      sleep(500);
    }
    fail("Workflow did not enter state " + SlowWorkflow.State.process.name());
  }

  @Test
  public void t03_interruptWorkflowWithSignal() {
    assertThat(setSignal(resp.id, SlowWorkflow.SIGNAL_INTERRUPT, "Setting signal via REST API"),
        is("Signal was set successfully"));
  }

  @Test
  public void t04_checkSlowWorkflowIsInterrupted() throws Exception {
    for (int i = 0; i < 20; i++) {
      ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
      if (SlowWorkflow.State.interrupted.name().equals(wf.state)) {
        return;
      }
      sleep(500);
    }
    fail("Workflow instance was not interrupted");
  }

  @Test
  public void t05_checkWorkflowActions() {
    ListWorkflowInstanceResponse wf = getWorkflowInstance(resp.id);
    assertThat(wf.actions.size(), is(4));
    Action action = wf.actions.get(0);
    assertThat(action.stateText, is("Interrupted with signal 1, moving to interrupted state"));
    assertThat(action.state, is(SlowWorkflow.State.process.name()));
    assertThat(action.type, is(WorkflowActionType.stateExecution.name()));
    action = wf.actions.get(1);
    assertThat(action.stateText, is("Clearing signal from process state"));
    assertThat(action.state, is(SlowWorkflow.State.process.name()));
    assertThat(action.type, is(WorkflowActionType.stateExecution.name()));
    action = wf.actions.get(2);
    assertThat(action.stateText, is("Setting signal via REST API"));
    assertThat(action.state, is(SlowWorkflow.State.process.name()));
    assertThat(action.type, is(WorkflowActionType.externalChange.name()));
  }

}
