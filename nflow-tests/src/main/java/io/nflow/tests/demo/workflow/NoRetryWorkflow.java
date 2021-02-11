package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import java.util.function.BiFunction;

import org.springframework.stereotype.Component;

import io.nflow.engine.exception.StateProcessExceptionHandling;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.NonRetryable;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@Component
public class NoRetryWorkflow extends AbstractWorkflowDefinition {

  public static final String TYPE = "noRetry";

  public static enum State implements WorkflowState {
    begin(start, "Retry always disabled for this state"), //
    process(normal, "Retry disabled for exceptions annotated with @NonRetryable"), //
    done(end, "End state"), //
    error(manual, "Error state");

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

  public NoRetryWorkflow() {
    super(TYPE, State.begin, State.error, new WorkflowSettings.Builder().setExceptionAnalyzer(exceptionAnalyzer()).build());
    setDescription("Workflow demonstrating how to avoid automatic retry");
    permit(State.begin, State.process);
    permit(State.process, State.done);
  }

  private static BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> exceptionAnalyzer() {
    return (s, t) -> new StateProcessExceptionHandling.Builder()
        .setRetryable(s != State.begin && !t.getClass().isAnnotationPresent(NonRetryable.class)).build();
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
