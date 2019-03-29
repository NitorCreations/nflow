package io.nflow.engine.processing.annotation;

import io.nflow.engine.processing.NextProcessingAction;
import io.nflow.engine.processing.WorkflowProcessingDefinition;
import io.nflow.engine.processing.WorkflowProcessingInstance;
import io.nflow.engine.processing.WorkflowProcessingState;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationWorkflowProcessingInstance implements WorkflowProcessingInstance {
    private final Class<?> implClass;
    private final WorkflowInstance workflowInstance;
    private final AnnotationWorkflowProcessingDefinition definition;
    private final AnnotationWorkflowProcessingState currentState;
    private final Method stateMethod;

    public AnnotationWorkflowProcessingInstance(Class<?> implClass,
                                                AnnotationWorkflowProcessingDefinition definition,
                                                WorkflowInstance workflowInstance,
                                                AnnotationWorkflowProcessingState currentState,
                                                Method stateMethod) {
        this.implClass = implClass;
        this.definition = definition;
        this.workflowInstance = workflowInstance;
        this.currentState = currentState;
        this.stateMethod = stateMethod;
    }

    @Override
    public WorkflowProcessingDefinition getWorkflowDefinition() {
        return definition;
    }

    @Override
    public WorkflowProcessingState getCurrentState() {
        return currentState;
    }

    @Override
    public NextProcessingAction executeState(StateExecution stateExecution) {
        try {
            Object object = implClass.getConstructors()[0].newInstance();
            if (stateMethod == null) {
                // e.g. database has state that is not present in current code
                throw new RuntimeException("Did not find method for state " + workflowInstance.state);
            }
            return callStateMethod(stateMethod, object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private NextProcessingAction callStateMethod(Method method, Object object)
            throws InvocationTargetException, IllegalAccessException {
        // TODO StateExecution params, StateVars
        return (NextProcessingAction) method.invoke(object);
    }

    @Override
    public int getRetryCount() {
        return workflowInstance.retries;
    }

    @Override
    public DateTime nextRetryTime() {
        return workflowInstance.nextActivation;
    }
}
