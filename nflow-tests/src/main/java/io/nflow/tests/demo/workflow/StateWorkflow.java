package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static java.lang.String.format;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.joda.time.Duration.millis;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;

@Component
public class StateWorkflow extends AbstractWorkflowDefinition {

  public static final String STATE_WORKFLOW_TYPE = "stateWorkflow";
  public static final String STATEVAR_QUERYTEST = "queryTest";

  public static final WorkflowState STATE_1 = new State("state1", start, "Set variable 1");
  public static final WorkflowState STATE_2 = new State("state2", "Set variable 2");
  public static final WorkflowState STATE_3 = new State("state3", "Update variable 2");
  public static final WorkflowState STATE_4 = new State("state4", "Do nothing");
  public static final WorkflowState STATE_5 = new State("state5", "Update variable 2");

  public StateWorkflow() {
    super(STATE_WORKFLOW_TYPE, STATE_1, ERROR, new WorkflowSettings.Builder().setMinErrorTransitionDelay(millis(0))
        .setMaxErrorTransitionDelay(millis(0)).setShortTransitionDelay(millis(0)).setMaxRetries(3).build());
    setDescription("Workflow for testing state variables");
    permit(STATE_1, STATE_2);
    permit(STATE_2, STATE_3);
    permit(STATE_3, STATE_4);
    permit(STATE_4, STATE_5);
    permit(STATE_5, DONE);
  }

  public NextAction state1(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "variable1", instantiateIfNotExists = true) Variable variable1) {
    variable1.value = "foo1";
    execution.setVariable(STATEVAR_QUERYTEST, "oldValue");
    return moveToState(STATE_2, "variable1 is set to " + variable1.value);
  }

  public NextAction state2(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "variable2", instantiateIfNotExists = true) Variable variable2) {
    variable2.value = "bar1";
    execution.setVariable(STATEVAR_QUERYTEST, "anotherOldValue");
    return moveToState(STATE_3, "variable1 is set to " + variable2.value);
  }

  public NextAction state3(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "variable2") Variable variable2) {
    variable2.value = "bar2";
    execution.setVariable(STATEVAR_QUERYTEST, "newValue");
    return moveToState(STATE_4, "variable2 is set to " + variable2.value);
  }

  public NextAction state4(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "variable1") Variable variable1,
      @StateVar(value = "variable2") Variable variable2) {
    return moveToState(STATE_5, format("variable1=%s variable2=%s", variable1, variable2));
  }

  public NextAction state5(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "variable2") Variable variable2) {
    variable2.value = "bar3";
    return moveToState(DONE, "variable2 is set to " + variable2.value);
  }

  public void done(@SuppressWarnings("unused") StateExecution execution) {
    System.out.println("StateWorkflow done.");
  }

  public static class Variable {
    public String value;

    @Override
    public String toString() {
      return reflectionToString(this);
    }
  }
}
