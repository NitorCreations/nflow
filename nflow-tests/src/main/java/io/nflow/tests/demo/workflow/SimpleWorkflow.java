package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class SimpleWorkflow extends WorkflowDefinition<SimpleWorkflow.State> {

  public static final String SIMPLE_WORKFLOW_TYPE = "simple";

  public static enum State implements WorkflowState {
    begin(start), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }
  }

  public SimpleWorkflow() {
    super(SIMPLE_WORKFLOW_TYPE, State.begin, State.error);
    setDescription("Simple demo workflow: start -> done");
    permit(State.begin, State.done);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Finished");
  }
}
