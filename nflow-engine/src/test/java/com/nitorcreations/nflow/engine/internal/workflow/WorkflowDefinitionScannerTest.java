package com.nitorcreations.nflow.engine.internal.workflow;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;


public class WorkflowDefinitionScannerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  WorkflowDefinitionScanner scanner = new WorkflowDefinitionScanner();

  @Test
  public void overloadingStateMethodShouldThrowException() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("OverloadedStateMethodWorkflow.end");
    thrown.expectMessage("Overloading state methods is not allowed.");
    scanner.getStateMethods(OverloadedStateMethodWorkflow.class);
  }
  @Test
  public void missingStateVarAnnotationShouldThrowException() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("MissingStateVarWorkflow.end");
    thrown.expectMessage("missing @StateVar annotation");
    scanner.getStateMethods(MissingStateVarWorkflow.class);
  }

  public static enum ScannerState implements WorkflowState{
    start(WorkflowStateType.start),
    end(WorkflowStateType.end);
    private final WorkflowStateType type;

    private ScannerState(WorkflowStateType type) {
      this.type = type;
    }
    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getName() {
      return name();
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  public static class OverloadedStateMethodWorkflow extends WorkflowDefinition<ScannerState> {
    public OverloadedStateMethodWorkflow() {
      super("overload", ScannerState.start, ScannerState.end);
    }

    public void start(StateExecution exec) { }
    public void end(StateExecution exec) { }
    public void end(StateExecution exec, @StateVar("foo") String param) { }
  }

  public static class MissingStateVarWorkflow extends WorkflowDefinition<ScannerState> {
    public MissingStateVarWorkflow() {
      super("overload", ScannerState.start, ScannerState.end);
    }

    public void start(StateExecution exec) { }
    public void end(StateExecution exec, String param) { }
  }

}
