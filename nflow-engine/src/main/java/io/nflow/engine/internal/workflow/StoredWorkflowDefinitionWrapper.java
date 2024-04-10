package io.nflow.engine.internal.workflow;

import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.util.Collection;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class StoredWorkflowDefinitionWrapper extends WorkflowDefinition {
    @SuppressWarnings("this-escape")
    public StoredWorkflowDefinitionWrapper(StoredWorkflowDefinition stored) {
        super(stored.type, getInitialState(stored), getErrorState(stored), new WorkflowSettings.Builder().build(), emptyMap(), allStates(stored), false);
        setDescription(stored.description);
    }

    private static Collection<WorkflowState> allStates(StoredWorkflowDefinition stored) {
        return stored.states.stream().map(StoredWorkflowDefinitionWrapper::toState).collect(toList());
    }

    private static WorkflowState getInitialState(StoredWorkflowDefinition stored) {
      return stored.states.stream()
          .filter(state -> start.name().equals((state.type)))
          .findFirst()
          .map(StoredWorkflowDefinitionWrapper::toState)
          .orElseThrow(() -> new IllegalStateException("Could not find initial state for " + stored));
    }

    private static WorkflowState toState(StoredWorkflowDefinition.State state) {
      return new State(state.id, WorkflowStateType.valueOf(state.type), state.description);
    }

    private static WorkflowState getErrorState(StoredWorkflowDefinition stored) {
      return stored.states.stream()
          .filter(state -> stored.onError.equals((state.id)))
          .findFirst()
          .map(StoredWorkflowDefinitionWrapper::toState)
          .orElseThrow(() -> new IllegalStateException("Could not find error state for " + stored));
    }
}
