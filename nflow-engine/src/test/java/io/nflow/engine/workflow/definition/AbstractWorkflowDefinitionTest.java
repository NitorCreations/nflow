package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.workflow.StaticStateFieldsWorkflow;
import io.nflow.engine.workflow.curated.State;
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
      permit(TestState.PROCESS, TestState.DONE);
      permit(TestState.PROCESS, TestState.DONE, TestWorkflow.FAILED);
      permit(TestState.PROCESS, TestState.ERROR, TestState.ERROR);
    }

    public void permitSameFailure() {
      permit(TestState.PROCESS, TestState.DONE);
      permit(TestState.PROCESS, TestState.DONE, TestWorkflow.FAILED);
      permit(TestState.PROCESS, TestState.ERROR, TestWorkflow.FAILED);
    }
  }

  static class TestWorkflow3 extends TestWorkflow {
    public NextAction done(@SuppressWarnings("unused") StateExecution execution) {
      return stopInState(TestState.DONE, "Done");
    }
  }

  static class TestWorkflow4 extends WorkflowDefinition {

    protected TestWorkflow4() {
      super("test", TestState.BEGIN, TestState.ERROR);
    }

    public void begin(@SuppressWarnings("unused") StateExecution execution) {
      // do nothing
    }
  }

  @Test
  public void isAllowedNextActionReturnsFalseForIllegalStateChange() {
    WorkflowInstance instance = new WorkflowInstance.Builder().setState("begin").build();
    NextAction nextAction = moveToState(TestState.PROCESS, "reason");
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
    NextAction nextAction = moveToState(TestState.ERROR, "reason");
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
    NextAction nextAction = moveToState(TestState.DONE, "reason");
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
    assertThat(wf.getInitialState(), is(TestState.BEGIN));
  }

  @Test
  public void getSupportedSignalsReturnsEmptyMap() {
    assertThat(workflow.getSupportedSignals(), is(emptyMap()));
  }

  @Test
  public void registersWorkflowStates() {
    Set<String> registeredStateNames = new StaticStateFieldsWorkflow().getStates().stream().map(WorkflowState::name)
        .collect(toSet());
    assertThat(registeredStateNames, containsInAnyOrder(TestState.BEGIN.name(), TestState.ERROR.name(), "origin", "target",
        "failure", "register", "staticPublic1", "staticPublic2"));
  }

  @Test
  public void finalStateCannotBeRegisteredWithStateMethodThatReturnsValue() {
    WorkflowDefinition wf = new StaticStateFieldsWorkflow();
    assertThrows(IllegalArgumentException.class,
        () -> wf.registerState(new State(TestState.BEGIN.name(), WorkflowStateType.end)));
  }

  @Test
  public void nonFinalStateCannotBeRegisteredWithStateMethodThatDoesNotReturnsNextAction() {
    WorkflowDefinition wf = new StaticStateFieldsWorkflow();
    assertThrows(IllegalArgumentException.class, () -> wf.registerState(new State("invalidReturnValue")));
    assertThrows(IllegalArgumentException.class, () -> wf.registerState(new State("invalidParameters")));
  }
}
