package com.nitorcreations.nflow.engine.workflow;

import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.manual;

import java.util.EnumSet;
import java.util.Set;

public abstract class WorkflowDefinition<S extends Enum<S> & WorkflowState> extends AbstractWorkflowDefinition<S> {

  private final Set<S> allStates;

  protected WorkflowDefinition(String type, S initialState, S errorState) {
    this(type, initialState, errorState, new WorkflowSettings(null));
  }

  @SuppressWarnings("unchecked")
  protected WorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    super(type, initialState, errorState, settings);
    this.allStates = EnumSet.allOf((Class<S>) initialState.getClass());
    requireEnumValuesHaveMatchingMethod();
  }

  private void requireEnumValuesHaveMatchingMethod() {
    for (S state : allStates) {
      if (state.getType() != manual && state.getType() != end) {
        requireStateMethodExists(state);
      }
    }
  }

  @Override
  public Set<S> getStates() {
    return allStates;
  }
}
