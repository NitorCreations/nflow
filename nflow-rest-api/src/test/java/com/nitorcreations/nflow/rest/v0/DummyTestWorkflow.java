package com.nitorcreations.nflow.rest.v0;

import static com.nitorcreations.nflow.rest.v0.DummyTestWorkflow.State.end;
import static com.nitorcreations.nflow.rest.v0.DummyTestWorkflow.State.error;
import static com.nitorcreations.nflow.rest.v0.DummyTestWorkflow.State.start;

import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.State> {

  public static enum State implements com.nitorcreations.nflow.engine.workflow.WorkflowState {
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

  public void start(StateExecution execution) {
    execution.setNextState(end);
  }

  public void error(StateExecution execution) {
    execution.setNextState(error);
  }

  public void end(StateExecution execution) {
    execution.setNextState(end);
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
