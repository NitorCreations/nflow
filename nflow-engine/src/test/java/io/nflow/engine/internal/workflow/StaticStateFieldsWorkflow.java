package io.nflow.engine.internal.workflow;

import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.ERROR;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@SuppressWarnings("unused")
public class StaticStateFieldsWorkflow extends WorkflowDefinition {
  private static WorkflowState staticPrivateState = new State("staticPrivate", WorkflowStateType.manual);
  static WorkflowState staticPackageProtectedState = new State("staticPackageProtected", WorkflowStateType.manual);
  protected static WorkflowState staticProtectedState = new State("staticProtected", WorkflowStateType.manual);
  public static WorkflowState staticPublicState1 = new State("staticPublic1", WorkflowStateType.manual);
  public static WorkflowState staticPublicState2 = new State("staticPublic2", WorkflowStateType.manual);
  private final WorkflowState privateState = new State("private", WorkflowStateType.manual);
  WorkflowState packageProtectedState = new State("packageProtected", WorkflowStateType.manual);
  protected WorkflowState protectedState = new State("protected", WorkflowStateType.manual);
  public WorkflowState publicState = new State("public", WorkflowStateType.manual);

  @SuppressWarnings("this-escape")
  public StaticStateFieldsWorkflow() {
    super("staticStateFields", BEGIN, ERROR);
    permit(new State("origin", WorkflowStateType.manual), new State("target", WorkflowStateType.manual),
        new State("failure", WorkflowStateType.manual));
    registerState(new State("register", WorkflowStateType.manual));
  }

  public NextAction begin(StateExecution exec) {
    return null;
  }

  public void done(StateExecution exec, @StateVar(value = "paramKey", readOnly = true) String param) {
    // do nothing
  }

  public String invalidReturnValue(StateExecution exec) {
    return null;
  }

  public NextAction invalidParameters() {
    return null;
  }
}
