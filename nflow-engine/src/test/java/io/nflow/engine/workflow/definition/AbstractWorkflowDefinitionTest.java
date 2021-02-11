package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.nflow.engine.workflow.instance.WorkflowInstance;

public class AbstractWorkflowDefinitionTest {

  private final TestWorkflow workflow = new TestWorkflow();

  @BeforeEach
  public void setup() {
    setCurrentMillisFixed(DateTime.now().getMillis());
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void sameFailureStateCanBePermittedAgain() {
    new TestWorkflow2().permitSameFailure();
  }

  @Test
  public void onlyOneFailureStateCanBeDefined() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> new TestWorkflow2().permitDifferentFailure());
    assertThat(thrown.getMessage(), containsString("Different failureState 'failed' already defined for originState 'process'"));
  }

  static class TestWorkflow2 extends TestWorkflow {
    public void permitDifferentFailure() {
      permit(TestWorkflow.PROCESS, TestWorkflow.DONE);
      permit(TestWorkflow.PROCESS, TestWorkflow.DONE, TestWorkflow.FAILED);
      permit(TestWorkflow.PROCESS, TestWorkflow.ERROR, TestWorkflow.ERROR);
    }

    public void permitSameFailure() {
      permit(TestWorkflow.PROCESS, TestWorkflow.DONE);
      permit(TestWorkflow.PROCESS, TestWorkflow.DONE, TestWorkflow.FAILED);
      permit(TestWorkflow.PROCESS, TestWorkflow.ERROR, TestWorkflow.FAILED);
    }
  }

  static class TestWorkflow3 extends TestWorkflow {
    public NextAction done(@SuppressWarnings("unused") StateExecution execution) {
      return stopInState(TestWorkflow.DONE, "Done");
    }
  }

  static class TestWorkflow4 extends AbstractWorkflowDefinition {

    protected TestWorkflow4() {
      super("test", TestWorkflow.BEGIN, TestWorkflow.ERROR);
    }

    public void begin(@SuppressWarnings("unused") StateExecution execution) {
      // do nothing
    }
  }

  @Test
  public void isAllowedNextActionReturnsFalseForIllegalStateChange() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestWorkflow.PROCESS, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(false));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForMovingToFailureState() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestWorkflow.FAILED, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForMovingToErrorState() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("process").build();
    NextAction nextAction = moveToState(TestWorkflow.ERROR, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForRetry() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = retryAfter(now().plusMinutes(1), "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForPermittedStateChange() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestWorkflow.DONE, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void nonFinalStateMethodMustReturnNextAction() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new TestWorkflow3());
    assertThat(thrown.getMessage(), containsString(
        "Class 'io.nflow.engine.workflow.definition.AbstractWorkflowDefinitionTest$TestWorkflow3' has a final state method 'done' that returns a value"));
  }

  @Test
  public void finalStateMethodMustReturnVoid() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new TestWorkflow4());
    assertThat(thrown.getMessage(), containsString(
        "Class 'io.nflow.engine.workflow.definition.AbstractWorkflowDefinitionTest$TestWorkflow4' has a non-final state method 'begin' that does not return NextAction"));
  }

  @Test
  public void settersAndGettersWork() {
    TestWorkflow wf = new TestWorkflow();
    wf.setName("name");
    wf.setDescription("description");
    assertThat(wf.getName(), is("name"));
    assertThat(wf.getDescription(), is("description"));
    assertThat(wf.getInitialState(), is(TestWorkflow.BEGIN));
  }

  @Test
  public void getSupportedSignalsReturnsEmptyMap() {
    assertThat(workflow.getSupportedSignals(), is(emptyMap()));
  }
}
