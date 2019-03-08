package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class Demo2Workflow extends WorkflowDefinition<Demo2Workflow.State> {

  public static final String DEMO2_WORKFLOW_TYPE = "demo2";

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

    @Override
    public String getDescription() {
      return name();
    }
  }

  public Demo2Workflow() {
    super(DEMO2_WORKFLOW_TYPE, State.begin, State.error);
    setDescription("Simple demo workflow: start -> process -> end");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(State.process, "Go to process state");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Go to done state");
  }
}
