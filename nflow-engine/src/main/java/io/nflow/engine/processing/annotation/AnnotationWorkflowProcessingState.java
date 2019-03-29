package io.nflow.engine.processing.annotation;

import io.nflow.engine.processing.WorkflowProcessingState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

import java.util.List;

public class AnnotationWorkflowProcessingState implements WorkflowProcessingState {
    private final Class<?> implClass;
    private final NflowWorkflow nflowWorkflow;
    private final NflowState nflowState;

    public AnnotationWorkflowProcessingState(Class<?> implClass, NflowWorkflow nflowWorkflow, NflowState nflowState) {
        this.implClass = implClass;
        this.nflowWorkflow = nflowWorkflow;
        this.nflowState = nflowState;
    }

    @Override
    public WorkflowStateType getType() {
        return nflowState.type();
    }

    @Override
    public String getName() {
        // TODO if displayName==""  use method name
        return nflowState.displayName();
    }

    @Override
    public String getDescription() {
        // TODO "" => null
        return nflowState.description();
    }

    @Override
    public List<WorkflowProcessingState> possileNextStates() {
        return null;
    }

    @Override
    public WorkflowProcessingState getFailureState() {

        return null;
    }

    @Override
    public int getMaxRetries() {
        if (nflowState.maxRetries() > -1) {
            return nflowState.maxRetries();
        }
        return nflowWorkflow.maxRetries();
    }
}
