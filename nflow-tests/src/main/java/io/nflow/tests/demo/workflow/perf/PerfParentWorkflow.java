package io.nflow.tests.demo.workflow.perf;

import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances.Builder;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.engine.workflow.definition.WorkflowStateType.wait;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static org.joda.time.DateTime.now;

@Component
public class PerfParentWorkflow extends WorkflowDefinition<PerfParentWorkflow.State> {

  public static final String TYPE = "perfParent";

  public static enum State implements WorkflowState {
    begin(start), parent1(start), parent2(normal), poll(wait), process(normal), done(end), error(manual);

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

  public PerfParentWorkflow() {
    super(TYPE, State.begin, State.error);
    setDescription("Perf parent workflow");
    permit(State.begin, State.parent1);
    permit(State.parent1, State.parent2);
    permit(State.parent2, State.poll);
    permit(State.poll, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(StateExecution execution, @StateVar("s") Mutable<String> s, @StateVar("i") Mutable<Integer> i) {
    s.setVal("p");
    i.setVal(0);
    return moveToState(State.parent1, "Begin");
  }

  public NextAction parent1(StateExecution execution, @StateVar("i") Mutable<Integer> i) {
    i.setVal(i.getVal() + 1);
    WorkflowInstance childWorkflow = new WorkflowInstance.Builder()
            .setType(PerfPlainWorkflow.TYPE)
            .setNextActivation(now())
            .setStateVariables(Map.of("init", "p1"))
            .build();
    execution.addChildWorkflows(childWorkflow);
    return moveToState(State.parent2, "Parent1");
  }

  public NextAction parent2(StateExecution execution, @StateVar("i") Mutable<Integer> i) {
    i.setVal(i.getVal() + 1);
    WorkflowInstance childWorkflow = new WorkflowInstance.Builder()
            .setType(PerfPlainWorkflow.TYPE)
            .setState(PerfPlainWorkflow.State.process1.name())
            .setNextActivation(now())
            .setStateVariables(Map.of("init", "p2"))
            .build();
    execution.addChildWorkflows(childWorkflow);
    return moveToState(State.poll, "Parent2");
  }

  public NextAction poll(StateExecution execution) {
    List<WorkflowInstance> children = execution.queryChildWorkflows(new Builder().setIncludeCurrentStateVariables(false).build());
    if (children.stream().map(child -> child.status).allMatch(status -> status == finished)) {
      return moveToState(State.process, "Parent2");
    }
    return retryAfter(now().plusMinutes(15), "Parent2 wait");
  }

  public NextAction process(StateExecution execution) {
    return moveToState(State.done, "Process");
  }
}
