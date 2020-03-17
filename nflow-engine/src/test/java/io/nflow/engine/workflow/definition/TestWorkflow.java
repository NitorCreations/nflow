package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

public class TestWorkflow extends WorkflowDefinition<TestWorkflow.State> {

  public TestWorkflow() {
    super("test", State.begin, State.error);
    permit(State.begin, State.done, State.failed);
    permit(State.startWithoutFailure, State.done);
  }

  public static enum State implements WorkflowState {
    begin(start), startWithoutFailure(start), process(normal), nonRetryable(normal, false), done(end), failed(end), error(manual);

    private WorkflowStateType stateType;
    private boolean isRetryAllowed;

    private State(WorkflowStateType stateType) {
      this(stateType, true);
    }

    private State(WorkflowStateType stateType, boolean isRetryAllowed) {
      this.stateType = stateType;
      this.isRetryAllowed = isRetryAllowed;
    }

    @Override
    public WorkflowStateType getType() {
      return stateType;
    }

    @Override
    public boolean isRetryAllowed() {
      return isRetryAllowed;
    }
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Done");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Done");
  }

  public NextAction nonRetryable(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Done");
  }

  public NextAction startWithoutFailure(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(State.done, "Done");
  }
}