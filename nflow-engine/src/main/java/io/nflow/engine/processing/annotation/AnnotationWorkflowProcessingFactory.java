package io.nflow.engine.processing.annotation;

import io.nflow.engine.processing.AbstractWorkflowProcessingFactory;
import io.nflow.engine.processing.WorkflowProcessingInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance;

import java.lang.reflect.Method;

public class AnnotationWorkflowProcessingFactory extends AbstractWorkflowProcessingFactory {

    @Override
    public WorkflowProcessingInstance createInstance(WorkflowInstance workflowInstance) {
        Class<?> implClass = findImplementingClass(workflowInstance);
        AnnotationWorkflowProcessingDefinition definition = new AnnotationWorkflowProcessingDefinition(implClass);

        NflowWorkflow[] annotations = implClass.getAnnotationsByType(NflowWorkflow.class);

        NflowWorkflow nflowWorkflow = annotations[0];
        NflowState nflowState = null;
        Method stateMethod = null;
        for (Method method : implClass.getMethods()) {
            NflowState methodAnnotation = method.getAnnotation(NflowState.class);
            if (methodAnnotation == null && !method.getName().equals(workflowInstance.state)) {
                continue;
            }
            nflowState = methodAnnotation;
            stateMethod = method;
            break;
        }
        // TODO it is possible that nflowState/stateMethod are null => current code doesn't support this state
        // engine should check for this
        AnnotationWorkflowProcessingState currentState = new AnnotationWorkflowProcessingState(implClass, nflowWorkflow, nflowState);
        return new AnnotationWorkflowProcessingInstance(implClass, definition, workflowInstance, currentState, stateMethod);
    }

    @Override
    public boolean appliesTo(WorkflowInstance workflowInstance) {
        return findImplementingClass(workflowInstance) != null;
    }

    private Class<?> findImplementingClass(WorkflowInstance workflowInstance) {
        // TODO how to implement this?
        return null;
    }
}
