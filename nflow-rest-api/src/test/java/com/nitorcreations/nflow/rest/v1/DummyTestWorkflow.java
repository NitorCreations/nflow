package com.nitorcreations.nflow.rest.v1;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.end;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.error;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.start;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.State> {

  public static enum State implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
    start(WorkflowStateType.start, "start desc"),
    error(WorkflowStateType.manual, "error desc"),
    end(WorkflowStateType.end, "end desc");

    private WorkflowStateType type;
    private String description;

    private State(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public DummyTestWorkflow() {
    super("dummy", start, error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(300).setMaxErrorTransitionDelay(1000).setShortTransitionDelay(200).setImmediateTransitionDelay(100).setMaxRetries(10).build());
    permit(start, end, error);
    permit(start, error);
    permit(error, end);
  }

  public NextAction start(StateExecution execution) {
    return moveToState(end, "Go to end state");
  }

  public void error(StateExecution execution) {}

  public void end(StateExecution execution) {}
}
