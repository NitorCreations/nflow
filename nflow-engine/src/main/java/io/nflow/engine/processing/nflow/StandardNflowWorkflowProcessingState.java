package io.nflow.engine.processing.nflow;

import io.nflow.engine.processing.WorkflowProcessingState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class StandardNflowWorkflowProcessingState implements WorkflowProcessingState {
    private final AbstractWorkflowDefinition<? extends WorkflowState> definition;
    private final WorkflowState state;

    public StandardNflowWorkflowProcessingState(AbstractWorkflowDefinition<? extends WorkflowState> definition,
                                                WorkflowState state) {
        this.definition = definition;
        this.state = state;
    }

    @Override
    public WorkflowStateType getType() {
        return state.getType();
    }

    @Override
    public String getName() {
        return state.name();
    }

    @Override
    public String getDescription() {
        return state.getDescription();
    }

    @Override
    public List<WorkflowProcessingState> possileNextStates() {
        List<WorkflowProcessingState> nextStates = new ArrayList<>();
        for (String nextStateName: definition.getAllowedTransitions().getOrDefault(state.name(), emptyList())) {
            nextStates.add(getProcessingState(nextStateName));
        }
        // TODO include failureTransitions here?
        return nextStates;
    }

    @Override
    public WorkflowProcessingState getFailureState() {
        WorkflowState workflowState = definition.getFailureTransitions().get(state.name());
        if (workflowState == null) {
            return null;
        }
        return new StandardNflowWorkflowProcessingState(definition, workflowState);
    }

    @Override
    public int getMaxRetries() {
        // TODO settings are not always specified
        return definition.getWorkflowSettings().maxRetries;
    }

    private StandardNflowWorkflowProcessingState getProcessingState(String stateName) {
        return new StandardNflowWorkflowProcessingState(definition, definition.getState(stateName));
    }
}
