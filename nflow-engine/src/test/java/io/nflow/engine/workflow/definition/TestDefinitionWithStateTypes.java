package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

public class TestDefinitionWithStateTypes extends WorkflowDefinition<TestDefinitionWithStateTypes.State> {

  public static enum State implements WorkflowState {
    initial(start), state1(normal), state2(normal), error(manual), done(end);

    private WorkflowStateType stateType;

    private State(WorkflowStateType stateType) {
      this.stateType = stateType;
    }

    @Override
    public WorkflowStateType getType() {
      return stateType;
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  public TestDefinitionWithStateTypes(String type, State initialState) {
    super(type, initialState, State.error);
    permit(State.initial, State.done, State.error);
    permit(State.initial, State.state1);
    permit(State.initial, State.state2);
    permit(State.state1, State.state2);
    permit(State.state2, State.done);
  }

  public NextAction initial(@SuppressWarnings("unused") StateExecution execution) {
    return null;
  }

  public NextAction state1(@SuppressWarnings("unused") StateExecution execution,
      @SuppressWarnings("unused") @StateVar("arg") String param) {
    return null;
  }

  public NextAction state2(@SuppressWarnings("unused") StateExecution execution) {
    return null;
  }
}
