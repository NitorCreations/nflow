package io.nflow.tests;

import static io.nflow.tests.demo.workflow.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.demo.workflow.SlowWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SignalWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public SignalWorkflowTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @Test
  @Order(1)
  public void startSlowWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = SLOW_WORKFLOW_TYPE;
    req.businessKey = "1";
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkSlowWorkflowIsRunning() throws Exception {
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
  @Order(3)
  public void interruptWorkflowWithSignal() {
    assertTrue(setSignal(resp.id, SlowWorkflow.SIGNAL_INTERRUPT, "Setting signal via REST API").setSignalSuccess);
  }

  @Test
  @Order(4)
  public void checkSlowWorkflowIsInterrupted() throws Exception {
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
  @Order(5)
  public void checkWorkflowActions() {
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
