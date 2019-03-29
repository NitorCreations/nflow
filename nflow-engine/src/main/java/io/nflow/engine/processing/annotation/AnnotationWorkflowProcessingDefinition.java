package io.nflow.engine.processing.annotation;

import io.nflow.engine.processing.WorkflowProcessingDefinition;
import io.nflow.engine.processing.WorkflowProcessingSettings;
import io.nflow.engine.processing.WorkflowProcessingState;

import java.lang.reflect.Method;
import java.util.List;

public class AnnotationWorkflowProcessingDefinition implements WorkflowProcessingDefinition {
    private final Class<?> implClass;
    private final NflowWorkflow annotation;

    public AnnotationWorkflowProcessingDefinition(Class<?> implClass) {
        this.implClass = implClass;
        NflowWorkflow[] annotations = this.implClass.getAnnotationsByType(NflowWorkflow.class);
        this.annotation = annotations[0];
        checkWorkflowValidity();
    }

    private void checkWorkflowValidity() {
        // TODO check that all states are found, with correct parameters etc
    }

    @Override
    public String getName() {
        return annotation.name();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }

    @Override
    public WorkflowProcessingState getDefaultInitialState() {
        return null;
    }

    @Override
    public WorkflowProcessingState getGenericErrorState() {
        return null;
    }

    @Override
    public List<WorkflowProcessingState> getStates() {
        // TODO
        NflowState[] stateAnnotations = this.implClass.getAnnotationsByType(NflowState.class);
        return null;
    }

    private AnnotationWorkflowProcessingState createWorkflowProcessingState(NflowState nflowState) {
        // TODO
        for (Method method : implClass.getMethods()) {
            NflowState state = method.getAnnotation(NflowState.class);
        }
        return new AnnotationWorkflowProcessingState(implClass, annotation, nflowState);
    }

    @Override
    public WorkflowProcessingState getState(String stateName) {
        // TODO
        return null;
    }

    @Override
    public WorkflowProcessingSettings getSettings() {
        // TODO fetch fields from this.annotation
        return new WorkflowProcessingSettings() {
        };
    }
}
