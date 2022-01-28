package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.TestDefinition.START_1;
import static io.nflow.engine.workflow.definition.TestDefinitionWithStateTypes.STATE_1;
import static io.nflow.engine.workflow.definition.TestDefinitionWithStateTypes.STATE_2;
import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;
import static io.nflow.engine.workflow.definition.TestState.ERROR;
import static io.nflow.engine.workflow.definition.TestState.POLL;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class WorkflowDefinitionTest {

  @Test
  public void initialStateIsRequired() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new WorkflowDefinition("withoutInitialState", null, ERROR) {
        });
    assertThat(thrown.getMessage(), containsString("initialState must not be null"));
  }

  @Test
  public void initialStateMustBeStartState() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new WorkflowDefinition("nonStartInitialState", DONE, ERROR) {
        });
    assertThat(thrown.getMessage(), containsString("initialState must be a start state"));
  }

  @Test
  public void errorStateIsRequired() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new WorkflowDefinition("withoutErrorState", BEGIN, null) {
        });
    assertThat(thrown.getMessage(), containsString("errorState must not be null"));
  }

  @Test
  public void getStatesWorks() {
    TestDefinitionWithStateTypes def = new TestDefinitionWithStateTypes("x", BEGIN);
    assertThat(def.getStates(), containsInAnyOrder(BEGIN, DONE, STATE_1, STATE_2, TestState.ERROR));
  }

  @Test
  public void succeedsWhenInitialStateMethodExist() {
    new TestDefinition("x", START_1);
  }

  @Test
  public void failsWhenInitialStateMethodDoesntExist() {
    assertThrows(IllegalArgumentException.class, () -> new TestDefinition("x", POLL));
  }

  @Test
  public void succeedWhenPermittingExistingOriginAndTargetState() {
    new TestDefinition("x", START_1).permit(START_1, DONE);
  }

  @Test
  public void failsWhenPermittingNonExistingOriginState() {
    assertThrows(IllegalArgumentException.class, () -> new TestDefinition("x", START_1).permit(POLL, DONE));
  }

  @Test
  public void failsWhenPermittingNonExistingTargetState() {
    assertThrows(IllegalArgumentException.class, () -> new TestDefinition("x", START_1).permit(START_1, POLL));
  }

  @Test
  public void failsWhenFailureStateMethodDoesNotExist() {
    TestDefinition workflow = new TestDefinition("x", START_1);

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> workflow.permit(START_1, DONE, POLL));
    assertThat(thrown.getMessage(), containsString("Class '" + workflow.getClass().getName()
        + "' is missing non-final state handling method 'public NextAction poll(StateExecution execution, ... args)'"));
  }

  @Test
  public void allowedTranstionsCanContainMultipleTargetStates() {
    WorkflowDefinition def = new TestDefinitionWithStateTypes("y", BEGIN);
    assertEquals(asList(DONE.name(), STATE_1.name(), STATE_2.name()), def.getAllowedTransitions().get(BEGIN.name()));
    assertEquals(TestState.ERROR, def.getFailureTransitions().get(BEGIN.name()));
  }

  @Test
  public void isStartStateWorks() {
    WorkflowDefinition workflow = new TestDefinitionWithStateTypes("y", BEGIN);

    assertThat(workflow.isStartState(BEGIN.name()), equalTo(true));
    assertThat(workflow.isStartState(STATE_1.name()), equalTo(false));
    assertThat(workflow.isStartState(STATE_2.name()), equalTo(false));
    assertThat(workflow.isStartState(ERROR.name()), equalTo(false));
    assertThat(workflow.isStartState(DONE.name()), equalTo(false));
  }
}
