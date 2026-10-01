package io.nflow.tests.demo.workflow;

import java.util.function.BiFunction;

import org.springframework.stereotype.Component;

import io.nflow.engine.exception.StateProcessExceptionHandling;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.NonRetryable;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;

@Component
public class NoRetryWorkflow extends WorkflowDefinition {

  public static final String TYPE = "noRetry";

  public NoRetryWorkflow() {
    super(TYPE, TestState.BEGIN, TestState.ERROR,
        new WorkflowSettings.Builder().setExceptionAnalyzer(exceptionAnalyzer()).build());
    setDescription("Workflow demonstrating how to avoid automatic retry");
    permit(TestState.BEGIN, TestState.PROCESS);
    permit(TestState.PROCESS, TestState.DONE);
  }

  private static BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> exceptionAnalyzer() {
    return (s, t) -> new StateProcessExceptionHandling.Builder()
        .setRetryable(s != TestState.BEGIN && !t.getClass().isAnnotationPresent(NonRetryable.class)).build();
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
