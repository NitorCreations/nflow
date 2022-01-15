package io.nflow.rest.v1;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class TestState {
  public static final WorkflowState BEGIN = new State("begin", WorkflowStateType.start);
  public static final WorkflowState PROCESS = new State("process");
  public static final WorkflowState POLL = new State("poll", WorkflowStateType.wait);
  public static final WorkflowState DONE = new State("done", WorkflowStateType.end);
  public static final WorkflowState ERROR = new State("error", WorkflowStateType.manual);
}
