package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@Component
public class BusinessKeyWorkflow extends WorkflowDefinition<BusinessKeyWorkflow.State> {

  public static final String BUSINESS_KEY_WORKFLOW_TYPE = "businessKeyWorkflow";

  public static enum State implements WorkflowState {
    begin(start), process(normal), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }
  }

  public BusinessKeyWorkflow() {
    super(BUSINESS_KEY_WORKFLOW_TYPE, State.begin, State.error);
    setDescription("Workflow that updates business key");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(State.process, "Go to process state");
  }

  public NextAction process(StateExecution execution) {
    execution.setBusinessKey("newBusinessKey");
    return stopInState(State.done, "Go to done state");
  }
}
