package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class SlowWorkflow extends WorkflowDefinition<SlowWorkflow.State>{

  public static final String SLOW_WORKFLOW_TYPE = "slowWorkflow";
  public static final int SIGNAL_INTERRUPT = 1;

  public static enum State implements WorkflowState {
    begin(start), process(normal), interrupted(end), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }
  }

  public SlowWorkflow() {
    super(SLOW_WORKFLOW_TYPE, State.begin, State.error);
    setDescription("Workflow for testing a state that has a long execution time");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(State.process, "Go to process state");
  }

  public NextAction process(StateExecution execution) throws InterruptedException {
    for (int i = 0; i < 50; i++) {
      TimeUnit.MILLISECONDS.sleep(200);
      if (execution.getSignal().isPresent()) {
        Integer signal = execution.getSignal().get();
        execution.setSignal(Optional.empty(), "Clearing signal from process state");
        return moveToState(State.interrupted, "Interrupted with signal " + signal + ", moving to interrupted state");
      }
    }
    return moveToState(State.done, "Go to done state");
  }

  @Override
  public Map<Integer, String> getSupportedSignals() {
    return singletonMap(SIGNAL_INTERRUPT, "Interrupted");
  }

}
