package com.nitorcreations.nflow.rest.v1;

import static com.nitorcreations.nflow.engine.workflow.definition.NextState.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextState.stopInState;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.end;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.error;
import static com.nitorcreations.nflow.rest.v1.DummyTestWorkflow.State.start;

import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.workflow.definition.NextState;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.State> {

  public static enum State implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
    start(WorkflowStateType.start, "start", "start desc"),
    error(WorkflowStateType.manual, "error", "error desc"),
    end(WorkflowStateType.end, "end", "end desc");

    private WorkflowStateType type;
    private String name;
    private String description;

    private State(WorkflowStateType type, String name, String description) {
      this.type = type;
      this.name = name;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getDescription() {
      return description;
    }

  }

  public DummyTestWorkflow() {
    super("dummy", start, error, new DummyTestSettings(null));
    permit(start, end, error);
    permit(start, error);
    permit(error, end);
  }

  public NextState start(StateExecution execution) {
    return moveToState(end, "Go to end state");
  }

  public NextState error(StateExecution execution) {
    return stopInState(error, "Finished in error state");
  }

  public NextState end(StateExecution execution) {
    return stopInState(end, "Finished in end state");
  }

  public static class DummyTestSettings extends WorkflowSettings {

    public DummyTestSettings(Environment env) {
      super(env);
    }

    @Override
    public int getErrorTransitionDelay() {
      return 300;
    }

    @Override
    public int getShortTransitionDelay() {
      return 200;
    }

    @Override
    public int getImmediateTransitionDelay() {
      return 100;
    }

    @Override
    public int getMaxRetries() {
      return 10;
    }

  }

}
