package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.NonRetryable;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.tests.demo.workflow.NoRetryWorkflow.State;

@Component
public class NoRetryWorkflow extends WorkflowDefinition<State> {

  public static final String TYPE = "noRetry";

  public static enum State implements WorkflowState {
    begin(start, "Retry always disabled for this state", false), //
    process(normal, "Retry disabled for exceptions annotated with @NonRetryable"), //
    done(end, "End state"), //
    error(manual, "Error state");

    private WorkflowStateType type;
    private String description;
    private boolean isRetryAllowed;

    private State(WorkflowStateType type, String description) {
      this(type, description, true);
    }

    private State(WorkflowStateType type, String description, boolean isRetryAllowed) {
      this.type = type;
      this.description = description;
      this.isRetryAllowed = isRetryAllowed;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public boolean isRetryAllowed(Throwable thrown) {
      return isRetryAllowed && WorkflowState.super.isRetryAllowed(thrown);
    }
  }

  public NoRetryWorkflow() {
    super(TYPE, State.begin, State.error);
    setDescription("Workflow demonstrating how to avoid automatic retry");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    throw new RuntimeException();
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    throw new NonRetryableException();
  }

  @NonRetryable
  static class NonRetryableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
