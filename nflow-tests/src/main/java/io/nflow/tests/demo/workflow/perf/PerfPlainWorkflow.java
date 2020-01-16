package io.nflow.tests.demo.workflow.perf;

import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import org.springframework.stereotype.Component;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

@Component
public class PerfPlainWorkflow extends WorkflowDefinition<PerfPlainWorkflow.State> {

  public static final String TYPE = "perfPlain";

  public static enum State implements WorkflowState {
    begin(start), process1(start), process2(normal), process3(normal), done(end), error(manual);

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

  public PerfPlainWorkflow() {
    super(TYPE, State.begin, State.error);
    setDescription("Perf plain workflow");
    permit(State.begin, State.process1);
    permit(State.process1, State.process2);
    permit(State.process2, State.process3);
    permit(State.process3, State.done);
  }

  public NextAction begin(StateExecution execution, @StateVar("v1") Mutable<String> s, @StateVar("v2") Mutable<Integer> i) {
    s.setVal("p");
    i.setVal(0);
    return moveToState(State.process1, "Begin");
  }

  public NextAction process1(StateExecution execution, @StateVar("v2") Mutable<Integer> i) {
    i.setVal(i.getVal() == null ? 0 : i.getVal() + 1);
    return moveToState(State.process2, "Process1");
  }

  public NextAction process2(StateExecution execution, @StateVar("v2") Mutable<Integer> i) {
    i.setVal(i.getVal() + 1);
    return moveToState(State.process3, "Process2");
  }

  public NextAction process3(StateExecution execution, @StateVar("v2") Mutable<Integer> i) {
    i.setVal(i.getVal() + 1);
    return moveToState(State.done, "Process3");
  }
}
