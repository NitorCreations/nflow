package io.nflow.rest.v1;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.rest.v1.DummyTestWorkflow.State.end;
import static io.nflow.rest.v1.DummyTestWorkflow.State.error;
import static io.nflow.rest.v1.DummyTestWorkflow.State.start;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.State> {

  public static enum State implements io.nflow.engine.workflow.definition.WorkflowState {
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
