package com.nitorcreations.nflow.tests.demo;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.*;

/**
 * Fibonacci series generator using recursive process.
 * Each step is handled by a new child workflow.
 */
public class FibonacciWorkflow extends WorkflowDefinition<FibonacciWorkflow.State> {
    public static final String WORKFLOW_TYPE = "fibonacci";
    private static final Logger log = LoggerFactory.getLogger(FibonacciWorkflow.class);

    public static enum State implements WorkflowState {
        begin(start), nMinus1(normal), nMinus2(normal), done(end), error(manual);

        private WorkflowStateType type;

        private State(WorkflowStateType type) {
            this.type = type;
        }

        @Override
        public WorkflowStateType getType() {
            return type;
        }

        @Override
        public String getName() {
            return name();
        }

        @Override
        public String getDescription() {
            return name();
        }
    }

    public FibonacciWorkflow() {
        super(WORKFLOW_TYPE, State.begin, State.error);
        permit(State.begin, State.nMinus2);
        permit(State.nMinus2, State.nMinus1);
        permit(State.nMinus2, State.done);
        permit(State.nMinus1, State.done);
    }

    public NextAction begin(StateExecution execution,  @StateVar(value="requestData", readOnly=true) int n) {
        log.info("Fibonacci step N = {}", n);
        return NextAction.moveToState(State.nMinus2, "Starting N = " + n );
    }

    public NextAction nMinus2(StateExecution execution, @StateVar(value="requestData", readOnly=true) int n) {
        return nextStep(n - 2, 2, State.nMinus1);
    }

    public NextAction nMinus1(StateExecution execution, @StateVar(value="requestData", readOnly = true) int n) {
        return nextStep(n - 1, 1, State.done);
    }

    private NextAction nextStep(int nextN, int offset, State nextState) {
        if(nextN < 1) {
            log.info("nextN = {}. skipping to done", nextN);
            return NextAction.moveToState(State.done, "N - " + offset + " = " + nextN + ". Going to end.");
        }
        log.info("Create child workflow N={}", nextN);
        List<WorkflowInstance> childWorkflows = Arrays.asList(createWorkflow(nextN));
        return NextAction.moveToState(nextState, childWorkflows, "Creating childWorkflow to process f(" + nextN + ")");
    }

    public void done(StateExecution execution,  @StateVar(value="requestData", readOnly=true) int n) {
        // TODO fetch N values from childs, and sum them
        log.info("We are done N = " + n);
    }

    private WorkflowInstance createWorkflow(int n) {
        // TODO these must be set
        // TODO must check that type exists
        WorkflowInstance child = new WorkflowInstance.Builder()
                .setType(FibonacciWorkflow.WORKFLOW_TYPE)
                .setStatus(WorkflowInstance.WorkflowInstanceStatus.created)
                .setExternalId("" + new Random().nextLong())
                .setState(FibonacciWorkflow.State.begin.name())
                .setNextActivation(DateTime.now())
                .setStateVariables(Collections.singletonMap("requestData", "" + n)).build();
        return child;
    }
}
