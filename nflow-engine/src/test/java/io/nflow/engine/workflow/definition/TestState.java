package io.nflow.engine.workflow.definition;

import io.nflow.engine.workflow.curated.State;

public class TestState {
  public static final WorkflowState BEGIN = new State("begin", WorkflowStateType.start);
  public static final WorkflowState PROCESS = new State("process");
  public static final WorkflowState POLL = new State("poll", WorkflowStateType.wait);
  public static final WorkflowState DONE = new State("done", WorkflowStateType.end);
  public static final WorkflowState ERROR = new State("error", WorkflowStateType.manual);
}
