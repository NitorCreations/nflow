package io.nflow.tests.demo.workflow;

import io.nflow.engine.workflow.definition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.*;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static org.joda.time.DateTime.now;

@Component
public class ForeverWaitingWorkflow extends WorkflowDefinition<ForeverWaitingWorkflow.State> {
    private static final Logger logger = LoggerFactory.getLogger(ForeverWaitingWorkflow.class);

    public enum State implements WorkflowState {
        begin(start), waiting(normal), done(end), error(manual);

        private WorkflowStateType type;

        State(WorkflowStateType type) {
            this.type = type;
        }

        @Override
        public WorkflowStateType getType() {
            return type;
        }

        @Override
        public String getDescription() {
            return name();
        }
    }

    public static final String FOREVER_WAITING_WORKFLOW_TYPE = "foreverWaiting";

    public ForeverWaitingWorkflow() {
        super(FOREVER_WAITING_WORKFLOW_TYPE, ForeverWaitingWorkflow.State.begin, ForeverWaitingWorkflow.State.error);
        setDescription("Workflow that waits for a year in 'waiting' state");
        permit(ForeverWaitingWorkflow.State.begin, ForeverWaitingWorkflow.State.waiting);
        permit(ForeverWaitingWorkflow.State.waiting, ForeverWaitingWorkflow.State.waiting);
    }

    public NextAction begin(StateExecution execution) {
        logger.info("in begin state");
        return moveToStateAfter(ForeverWaitingWorkflow.State.waiting, now().plusYears(1), "Move to waiting after an year");
    }

    public NextAction waiting(StateExecution execution) {
        logger.info("in waiting state");
        return moveToStateAfter(ForeverWaitingWorkflow.State.waiting, now().plusYears(1), "Move to waiting again after an year");
    }

}
