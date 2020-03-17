package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.executor.InvalidNextActionException;

public class NextActionTest {

  @Test
  public void stopInStartStateThrowsException() {
    Assertions.assertThrows(InvalidNextActionException.class,
            () -> NextAction.stopInState(TestState.initial, "stop reason"));
  }

  @Test
  public void stopInNormalStateThrowsException() {
    Assertions.assertThrows(InvalidNextActionException.class,
            () -> NextAction.stopInState(TestState.state1, "stop reason"));
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

  @Test
  public void isRetryReturnsTrueForRetry() {
    assertThat(retryAfter(now(), "reason").isRetry(), is(true));
  }

  @Test
  public void isRetryReturnsFalseForOtherActions() {
    assertThat(moveToState(TestState.done, "reason").isRetry(), is(false));
    assertThat(moveToStateAfter(TestState.done, now(), "reason").isRetry(), is(false));
    assertThat(stopInState(TestState.done, "reason").isRetry(), is(false));
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
  }
}
