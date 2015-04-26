package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.done;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.error;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.state1;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.state2;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.state3;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.state4;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.State.state5;
import static java.lang.String.format;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class StateWorkflow extends WorkflowDefinition<StateWorkflow.State> {

  public static final String STATE_WORKFLOW_TYPE = "stateWorkflow";

  public static enum State implements com.nitorcreations.nflow.engine.workflow.definition.WorkflowState {
    state1(start, "Set variable 1"),
    state2(normal, "Set variable 2"),
    state3(normal, "Update variable 2"),
    state4(normal, "Do nothing"),
    state5(normal, "Update variable 2"),
    done(end, "Finished"),
    error(manual, "Error state");

    private final WorkflowStateType type;
    private final String description;

    private State(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public StateWorkflow() {
    super(STATE_WORKFLOW_TYPE, state1, error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0)
        .setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
    permit(state1, state2);
    permit(state2, state3);
    permit(state3, state4);
    permit(state4, state5);
    permit(state5, done);
  }

  public NextAction state1(StateExecution execution, @StateVar(value = "variable1", instantiateIfNotExists = true) Variable variable1) {
    variable1.value = "foo1";
    return moveToState(state2, "variable1 is set to " + variable1.value);
  }

  public NextAction state2(StateExecution execution, @StateVar(value = "variable2", instantiateIfNotExists = true) Variable variable2) {
    variable2.value = "bar1";
    return moveToState(state3, "variable1 is set to " + variable2.value);
  }

  public NextAction state3(StateExecution execution, @StateVar(value = "variable2") Variable variable2) {
    variable2.value = "bar2";
    return moveToState(state4, "variable2 is set to " + variable2.value);
  }

  public NextAction state4(StateExecution execution, @StateVar(value = "variable1") Variable variable1,
      @StateVar(value = "variable2") Variable variable2) {
    return moveToState(state5, format("variable1=%s variable2=%s", variable1, variable2));
  }

  public NextAction state5(StateExecution execution, @StateVar(value = "variable2") Variable variable2) {
    variable2.value = "bar3";
    return moveToState(done, "variable2 is set to " + variable2.value);
  }

  public void done(StateExecution execution) {
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
