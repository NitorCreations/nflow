package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class SlowWorkflow extends WorkflowDefinition<SlowWorkflow.State>{

  public static final String WORKFLOW_TYPE = "slowWorkflow";

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

  public SlowWorkflow() {
    super(WORKFLOW_TYPE, State.begin, State.error);
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(StateExecution execution) {
    return moveToState(State.process, "Go to process state");
  }

  public NextAction process(StateExecution execution) throws InterruptedException {
    sleep(SECONDS.toMillis(10));
    return moveToState(State.done, "Go to done state");
  }
}
