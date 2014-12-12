package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.Matchers.containsElementsInAnyOrder;
import static com.nitorcreations.Matchers.reflectEquals;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import com.nitorcreations.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import com.nitorcreations.nflow.engine.service.DummyTestWorkflow;
import com.nitorcreations.nflow.engine.service.DummyTestWorkflow.DummyTestState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

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
    roundTrip(new ArrayList<String>());
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
    assertThat(stored.states.size(), is(convertedOriginal.states.size()));
    for (int i = 0; i < convertedOriginal.states.size(); i++) {
      assertThat(stored.states.get(i), reflectEquals(convertedOriginal.states.get(i)));
    }
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
      for (DummyTestState originalState : original.getStates()) {
        if (originalState.name().equals(convertedState.id)) {
          assertThat(convertedState.description, is(originalState.getDescription()));
          WorkflowState originalFailureTransition = original.getFailureTransitions().get(originalState.name());
          assertThat(convertedState.onFailure, is(originalFailureTransition != null ? originalFailureTransition.name() : null));
          List<String> originalStateTransitions = original.getAllowedTransitions().get(originalState.name());
          if (originalStateTransitions != null) {
            assertThat(convertedState.transitions, containsElementsInAnyOrder(originalStateTransitions));
          } else {
            assertThat(convertedState.transitions.size(), is(0));
          }
          foundMatchingState = true;
          break;
        }
      }
      assertThat("Not found match for state " + convertedState.id, foundMatchingState, is(true));
    }
  }
}
