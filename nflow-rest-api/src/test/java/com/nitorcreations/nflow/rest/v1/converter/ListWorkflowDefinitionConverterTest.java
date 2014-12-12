package com.nitorcreations.nflow.rest.v1.converter;

import static com.nitorcreations.Matchers.reflectEquals;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.end;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.error;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.start;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import com.nitorcreations.nflow.rest.v1.DummyTestWorkflow;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v1.msg.State;

@RunWith(MockitoJUnitRunner.class)
public class ListWorkflowDefinitionConverterTest {

  private ListWorkflowDefinitionConverter converter;

  @Before
  public void setup() {
    converter = new ListWorkflowDefinitionConverter();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void convertWorks() {
    DummyTestWorkflow def = new DummyTestWorkflow();
    ListWorkflowDefinitionResponse resp = converter.convert(def);
    assertThat(resp.type, is(def.getType()));
    assertThat(resp.name, is(def.getName()));
    assertThat(resp.description, is(def.getDescription()));
    assertThat(resp.onError, is(def.getErrorState().name()));
    assertThat(resp.states, arrayContainingInAnyOrder(
        reflectEquals(getResponseState(end, Collections.<String>emptyList(), null)),
        reflectEquals(getResponseState(error, asList(end.name()), null)),
        reflectEquals(getResponseState(start, asList(end.name(), error.name()), error.name()))));
    assertThat((int)resp.settings.transitionDelaysInMilliseconds.immediate, is(def.getSettings().immediateTransitionDelay));
    assertThat((int)resp.settings.transitionDelaysInMilliseconds.waitShort, is(def.getSettings().shortTransitionDelay));
    assertThat((int)resp.settings.transitionDelaysInMilliseconds.minErrorWait, is(def.getSettings().minErrorTransitionDelay));
    assertThat((int)resp.settings.transitionDelaysInMilliseconds.maxErrorWait, is(def.getSettings().maxErrorTransitionDelay));
    assertThat(resp.settings.maxRetries, is(def.getSettings().maxRetries));
  }

  private State getResponseState(DummyTestWorkflow.State workflowState,
      List<String> nextStateNames, String errorStateName) {
    State state = new State(workflowState.name(),
        workflowState.getType().name(), workflowState.getDescription());
    state.transitions.addAll(nextStateNames);
    state.onFailure = errorStateName;
    return state;
  }

  @Test
  public void convertStoredDefinitionWorks() {
    StoredWorkflowDefinition stored = new StoredWorkflowDefinition();
    stored.description = "desc";
    stored.onError = "errorState";
    stored.type = "storedDefinition";
    StoredWorkflowDefinition.State storedState = new StoredWorkflowDefinition.State("first", "normal", "first state desc");
    stored.states = asList(storedState);
    ListWorkflowDefinitionResponse resp = converter.convert(stored);
    assertThat(resp.description, is(stored.description));
    assertThat(resp.onError, is(stored.onError));
    assertThat(resp.states.length, is(1));
    assertThat(resp.states[0].description, is(storedState.description));
    assertThat(resp.states[0].type, is(storedState.type));
    assertThat(resp.states[0].id, is(storedState.id));
  }
}
