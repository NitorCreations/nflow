package io.nflow.engine.workflow.definition;

import static io.nflow.engine.workflow.definition.NextAction.stopInState;

import io.nflow.engine.workflow.curated.SimpleState;

public class TestWorkflow extends AbstractWorkflowDefinition<WorkflowState> {

  public static final WorkflowState BEGIN = new SimpleState("begin", WorkflowStateType.start);
  public static final WorkflowState START_WITHOUT_FAILURE = new SimpleState("startWithoutFailure", WorkflowStateType.start);
  public static final WorkflowState PROCESS = new SimpleState("process");
  public static final WorkflowState DONE = new SimpleState("done", WorkflowStateType.end);
  public static final WorkflowState FAILED = new SimpleState("failed", WorkflowStateType.end);
  public static final WorkflowState ERROR = new SimpleState("error", WorkflowStateType.manual);

  public TestWorkflow() {
    super("test", BEGIN, ERROR);
    permit(BEGIN, DONE, FAILED);
    permit(START_WITHOUT_FAILURE, DONE);
  }

  public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }

  public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }

  public NextAction startWithoutFailure(@SuppressWarnings("unused") StateExecution execution) {
    return stopInState(DONE, "Done");
  }
}