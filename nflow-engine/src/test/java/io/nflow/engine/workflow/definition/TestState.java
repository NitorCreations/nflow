package io.nflow.engine.workflow.definition;

import io.nflow.engine.workflow.curated.SimpleState;

public class TestState {
  public static final WorkflowState BEGIN = new SimpleState("begin", WorkflowStateType.start);
  public static final WorkflowState PROCESS = new SimpleState("process");
  public static final WorkflowState POLL = new SimpleState("poll", WorkflowStateType.wait);
  public static final WorkflowState DONE = new SimpleState("done", WorkflowStateType.end);
  public static final WorkflowState ERROR = new SimpleState("error", WorkflowStateType.manual);
}
