package io.nflow.engine.internal.workflow;

import io.nflow.engine.internal.workflow.StoredWorkflowDefinition.State;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

import java.util.Collection;

import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class StoredWorkflowDefinitionWrapper extends WorkflowDefinition {
    public StoredWorkflowDefinitionWrapper(StoredWorkflowDefinition stored) {
        super(stored.type, getInitialState(stored), getErrorState(stored), new WorkflowSettings.Builder().build(), emptyMap(), allStates(stored), false);
        setDescription(stored.description);
    }

    private static Collection<WorkflowState> allStates(StoredWorkflowDefinition stored) {
        return stored.states.stream().map(StoredWorkflowDefinitionWrapper::toState).collect(toList());
    }

    private static WorkflowState getInitialState(StoredWorkflowDefinition stored) {
        for (State state : stored.states) {
            if (start.name().equals(state.type)) {
                return toState(state);
            }
        }
        throw new IllegalStateException("Could not find initial state for " + stored);
    }

    private static WorkflowState toState(StoredWorkflowDefinition.State state) {
        return new io.nflow.engine.workflow.curated.State(state.id, WorkflowStateType.valueOf(state.type), state.description);
    }

    private static WorkflowState getErrorState(StoredWorkflowDefinition stored) {
        for (State state : stored.states) {
            if (stored.onError.equals(state.id)) {
                return toState(state);
            }
        }
        throw new IllegalStateException("Could not find error state for " + stored);
    }
}
