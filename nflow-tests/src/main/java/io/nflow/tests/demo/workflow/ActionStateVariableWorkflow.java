package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.workflow.ActionStateVariableWorkflow.State.done;
import static io.nflow.tests.demo.workflow.ActionStateVariableWorkflow.State.error;
import static io.nflow.tests.demo.workflow.ActionStateVariableWorkflow.State.setVariable;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@Component
public class ActionStateVariableWorkflow extends WorkflowDefinition<ActionStateVariableWorkflow.State> {

  public static final String WORKFLOW_TYPE = "actionStateVariableWorkflow";
  public static final int MAX_STATE_VAR_VALUE = 10;
  private static final String STATE_VAR = "stateVar";

  public static enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    setVariable(start), done(end), error(manual);

    private final WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }
  }

  public ActionStateVariableWorkflow() {
    super(WORKFLOW_TYPE, setVariable, error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0)
        .setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
    setDescription("Workflow for testing action state variables");
    permit(setVariable, setVariable);
    permit(setVariable, done);
  }

  public NextAction setVariable(StateExecution execution,
      @StateVar(value = STATE_VAR, instantiateIfNotExists = true) Mutable<Long> val) {
    if (val.getVal() >= MAX_STATE_VAR_VALUE) {
      execution.setCreateAction(false);
      return stopInState(done, "Done");
    }
    val.setVal(val.getVal() + 1);
    return moveToState(setVariable, "Continue");
  }
}
