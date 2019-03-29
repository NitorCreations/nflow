package io.nflow.engine.processing.annotation;

import io.nflow.engine.processing.NextProcessingAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@NflowWorkflow(
        name = "exampleAnnotationWorkflow",
        description = "An example of annotation usage",
        defaultStartState = "startState",
        errorState = "errorState",
        maxSubsequentStateExecutions = 10,
        maxRetries = 17)
public class AnnotationExampleWorkflow {

    @NflowState(
            type = WorkflowStateType.start,
            nextStates = "normalState",
            failureState = "error",
            maxRetries = 7)
    public NextProcessingAction startState(StateExecution stateExecution) {
        return null;
    }

    @NflowState(
            nextStates = {"manualState", "doneDone"},
            maxSubsequentExecutions = 3)
    public NextProcessingAction normalState(@StateVar("counter") Integer counter) {
        return null;
    }

    @NflowState(type = WorkflowStateType.manual)
    public NextProcessingAction manualState() {
        return null;
    }

    @NflowState(
            type = WorkflowStateType.wait,
            nextStates = "done")
    public NextProcessingAction waitState() {
        return null;
    }


    @NflowState(type = WorkflowStateType.end)
    public NextProcessingAction errorState() {
        return null;
    }


    @NflowState(type = WorkflowStateType.end)
    public NextProcessingAction doneState() {
        return null;
    }

}
