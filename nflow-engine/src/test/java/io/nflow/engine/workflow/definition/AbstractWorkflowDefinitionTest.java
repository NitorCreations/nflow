package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.nflow.engine.workflow.instance.WorkflowInstance;

public class AbstractWorkflowDefinitionTest {

  private final TestWorkflow workflow = new TestWorkflow();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    setCurrentMillisFixed(DateTime.now().getMillis());
  }

  @After
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void sameFailureStateCanBePermittedAgain() {
    new TestWorkflow2().permitSameFailure();
  }

  @Test
  public void onlyOneFailureStateCanBeDefined() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Different failureState 'failed' already defined for originState 'process'");
    new TestWorkflow2().permitDifferentFailure();
  }

  static class TestWorkflow2 extends TestWorkflow {
    public void permitDifferentFailure() {
      permit(State.process, State.done);
      permit(State.process, State.done, State.failed);
      permit(State.process, State.error, State.error);
    }

    public void permitSameFailure() {
      permit(State.process, State.done);
      permit(State.process, State.done, State.failed);
      permit(State.process, State.error, State.failed);
    }
  }

  static class TestWorkflow3 extends TestWorkflow {
    public NextAction done(@SuppressWarnings("unused") StateExecution execution) {
      return stopInState(State.done, "Done");
    }
  }

  static class TestWorkflow4 extends WorkflowDefinition<TestWorkflow4.State> {

    protected TestWorkflow4() {
      super("test", State.begin, State.error);
    }

    public static enum State implements WorkflowState {
      begin(start), error(manual);

      private WorkflowStateType stateType;

      private State(WorkflowStateType stateType) {
        this.stateType = stateType;
      }

      @Override
      public WorkflowStateType getType() {
        return stateType;
      }

      @Override
      public String getDescription() {
        return name();
      }
    }

    public void begin(@SuppressWarnings("unused") StateExecution execution) {
      // do nothing
    }
  }

  @Test
  public void isAllowedNextActionReturnsFalseForIllegalStateChange() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestWorkflow.State.process, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(false));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForMovingToFailureState() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestWorkflow.State.failed, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void isAllowedNextActionReturnsTrueForMovingToErrorState() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("process").build();
    NextAction nextAction = moveToState(TestWorkflow.State.error, "reason");
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
    NextAction nextAction = moveToState(TestWorkflow.State.done, "reason");
    assertThat(workflow.isAllowedNextAction(instance, nextAction), is(true));
  }

  @Test
  public void nonFinalStateMethodMustReturnNextAction() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Class 'io.nflow.engine.workflow.definition.AbstractWorkflowDefinitionTest$TestWorkflow3' has a final state method 'done' that returns a value");
    new TestWorkflow3();
  }

  @Test
  public void finalStateMethodMustReturnVoid() {
    thrown.expect(IllegalArgumentException.class);
    thrown
        .expectMessage("Class 'io.nflow.engine.workflow.definition.AbstractWorkflowDefinitionTest$TestWorkflow4' has a non-final state method 'begin' that does not return NextAction");
    new TestWorkflow4();
  }

  @Test
  public void settersAndGettersWork() {
    TestWorkflow wf = new TestWorkflow();
    wf.setName("name");
    wf.setDescription("description");
    assertThat(wf.getName(), is("name"));
    assertThat(wf.getDescription(), is("description"));
    assertThat(wf.getInitialState(), is(TestWorkflow.State.begin));
  }

  @Test
  public void getSupportedSignalsReturnsEmptyMap() {
    TestWorkflow wf = new TestWorkflow();
    assertThat(wf.getSupportedSignals(), is(emptyMap()));
  }

}
