package io.nflow.engine.internal.dao;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.workflow.definition.WorkflowState;
import jakarta.inject.Inject;

public class WorkflowDefinitionDaoTest extends BaseDaoTest {

  @Inject
  WorkflowDefinitionDao dao;

  private final DummyTestWorkflow original = new DummyTestWorkflow();

  @Test
  public void storeAndLoadRequestedDefinitionFromDatabaseWorks() {
    roundTrip(asList(original.getType()));
  }

  @Test
  public void storeAndLoadAllDefinitionsFromDatabaseWorks() {
    roundTrip(new ArrayList<>());
  }

  @Test
  public void storeAndUpdateDefinitionWorks() {
    roundTrip(asList(original.getType()));
    roundTrip(asList(original.getType()));
  }

  private void roundTrip(List<String> typeQueryParameters) {
    StoredWorkflowDefinition convertedOriginal = dao.convert(original);
    dao.storeWorkflowDefinition(original);
    List<StoredWorkflowDefinition> storedDefinitions = dao.queryStoredWorkflowDefinitions(typeQueryParameters);
    assertThat(storedDefinitions.size(), is(1));
    StoredWorkflowDefinition stored = storedDefinitions.get(0);
    assertThat(stored.type, is(convertedOriginal.type));
    assertThat(stored.onError, is(convertedOriginal.onError));
    assertThat(stored.description, is(convertedOriginal.description));
    assertThat(stored.states, contains(convertedOriginal.states.toArray()));
    assertThat(stored.supportedSignals, contains(convertedOriginal.supportedSignals.toArray()));
  }

  @Test
  public void convertStoredDefinitionWorks() {
    StoredWorkflowDefinition convertedOriginal = dao.convert(original);
    assertThat(convertedOriginal.type, is(original.getType()));
    assertThat(convertedOriginal.onError, is(original.getErrorState().name()));
    assertThat(convertedOriginal.description, is(original.getDescription()));
    assertThat(convertedOriginal.states.size(), is(original.getStates().size()));
    for (StoredWorkflowDefinition.State convertedState : convertedOriginal.states) {
      boolean foundMatchingState = false;
      for (WorkflowState originalState : original.getStates()) {
        if (originalState.name().equals(convertedState.id)) {
          assertThat(convertedState.description, is(originalState.getDescription()));
          WorkflowState originalFailureTransition = original.getFailureTransitions().get(originalState.name());
          assertThat(convertedState.onFailure, is(originalFailureTransition != null ? originalFailureTransition.name() : null));
          List<String> originalStateTransitions = original.getAllowedTransitions().get(originalState.name());
          if (originalStateTransitions != null) {
            assertThat(convertedState.transitions, containsInAnyOrder(originalStateTransitions.toArray()));
          } else {
            assertThat(convertedState.transitions.size(), is(0));
          }
          foundMatchingState = true;
          break;
        }
      }
      assertThat("Not found match for state " + convertedState.id, foundMatchingState, is(true));
    }
    assertThat(convertedOriginal.supportedSignals.size(), is(original.getSupportedSignals().size()));
    for (StoredWorkflowDefinition.Signal convertedSignal : convertedOriginal.supportedSignals) {
      boolean foundMatchingSignal = false;
      for (Entry<Integer, String> originalSignal : original.getSupportedSignals().entrySet()) {
        if (originalSignal.getKey().equals(convertedSignal.value)) {
          assertThat(convertedSignal.description, is(originalSignal.getValue()));
          foundMatchingSignal = true;
          break;
        }
      }
      assertThat("Not found match for signal " + convertedSignal.value, foundMatchingSignal, is(true));
    }
  }
}
