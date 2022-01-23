package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.executor.InvalidNextActionException;

public class NextActionTest {

  @Test
  public void stopInStartStateThrowsException() {
    assertThrows(InvalidNextActionException.class, () -> stopInState(TestState.BEGIN, "stop reason"));
  }

  @Test
  public void stopInNormalStateThrowsException() {
    assertThrows(InvalidNextActionException.class, () -> stopInState(TestState.PROCESS, "stop reason"));
  }

  @Test
  public void stopInManualStateSetsActivationToNull() {
    NextAction nextAction = stopInState(TestState.ERROR, "stop reason");
    assertThat(nextAction.getActivation(), is(nullValue()));
  }

  @Test
  public void stopInEndStateSetsActivationToNull() {
    NextAction nextAction = stopInState(TestState.DONE, "stop reason");
    assertThat(nextAction.getActivation(), is(nullValue()));
  }

  @Test
  public void isRetryReturnsTrueForRetry() {
    assertThat(retryAfter(now(), "reason").isRetry(), is(true));
  }

  @Test
  public void isRetryReturnsFalseForOtherActions() {
    assertThat(moveToState(TestState.DONE, "reason").isRetry(), is(false));
    assertThat(moveToStateAfter(TestState.DONE, now(), "reason").isRetry(), is(false));
    assertThat(stopInState(TestState.DONE, "reason").isRetry(), is(false));
  }
}
