package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.nitorcreations.nflow.engine.internal.executor.InvalidNextActionException;

public class NextActionTest {

  @Test(expected = InvalidNextActionException.class)
  public void stopInStartStateThrowsException() {
    NextAction.stopInState(TestState.initial, "stop reason");
  }

  @Test(expected = InvalidNextActionException.class)
  public void stopInNormalStateThrowsException() {
    NextAction.stopInState(TestState.state1, "stop reason");
  }

  @Test
  public void stopInManualStateSetsActivationToNull() {
    NextAction nextAction = NextAction.stopInState(TestState.error, "stop reason");
    assertThat(nextAction.getActivation(), is(nullValue()));
  }

  @Test
  public void stopInEndStateSetsActivationToNull() {
    NextAction nextAction = NextAction.stopInState(TestState.done, "stop reason");
    assertThat(nextAction.getActivation(), is(nullValue()));
  }

  static enum TestState implements WorkflowState {
    initial(start), state1(normal), error(manual), done(end);

    private final WorkflowStateType stateType;

    private TestState(WorkflowStateType stateType) {
      this.stateType = stateType;
    }

    @Override
    public WorkflowStateType getType() {
      return stateType;
    }

    @Override
    public String getName() {
      return name();
    }

    @Override
    public String getDescription() {
      return name();
    }
  }
}
