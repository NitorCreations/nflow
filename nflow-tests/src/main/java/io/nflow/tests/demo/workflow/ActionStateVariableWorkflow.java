package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static org.joda.time.Duration.millis;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@Component
public class ActionStateVariableWorkflow extends WorkflowDefinition {

  public static final String WORKFLOW_TYPE = "actionStateVariableWorkflow";
  public static final int MAX_STATE_VAR_VALUE = 10;
  private static final String STATE_VAR = "stateVar";

  private static final WorkflowState SET_VARIABLE = new State("setVariable", WorkflowStateType.start);

  public ActionStateVariableWorkflow() {
    super(WORKFLOW_TYPE, SET_VARIABLE, TestState.ERROR, new WorkflowSettings.Builder().setMinErrorTransitionDelay(millis(0))
        .setMaxErrorTransitionDelay(millis(0)).setShortTransitionDelay(millis(0)).setMaxRetries(3).build());
    setDescription("Workflow for testing action state variables");
    permit(SET_VARIABLE, SET_VARIABLE);
    permit(SET_VARIABLE, TestState.DONE);
  }

  public NextAction setVariable(StateExecution execution,
      @StateVar(value = STATE_VAR, instantiateIfNotExists = true) Mutable<Long> val) {
    if (val.getVal() >= MAX_STATE_VAR_VALUE) {
      execution.setCreateAction(false);
      return stopInState(TestState.DONE, "Done");
    }
    val.setVal(val.getVal() + 1);
    return moveToState(SET_VARIABLE, "Continue");
  }
}
