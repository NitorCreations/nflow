package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class DemoWorkflow extends WorkflowDefinition<DemoWorkflow.State> {

  public static enum State implements WorkflowState {
    start(WorkflowStateType.start), process(normal), done(end), error(manual);

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

  public DemoWorkflow() {
    super("demo", State.start, State.error);
    permit(State.start, State.process);
    permit(State.process, State.done);
  }

  public NextAction start(StateExecution execution) {
    return moveToState(State.process, "Go to process state");
  }

  public NextAction process(StateExecution execution) {
    return moveToState(State.done, "Go to done state");
  }

  public void done(StateExecution execution) {}
}
