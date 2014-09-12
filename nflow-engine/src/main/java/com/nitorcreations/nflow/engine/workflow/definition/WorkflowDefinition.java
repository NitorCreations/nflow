package com.nitorcreations.nflow.engine.workflow.definition;

import java.util.EnumSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The base class for enum based workflow definitions.
 * @param <S> The enumeration of valid workflow states.
 */
public abstract class WorkflowDefinition<S extends Enum<S> & WorkflowState> extends AbstractWorkflowDefinition<S> {

  private final Set<S> allStates;

  /**
   * Create a workflow definition.
   * @param type The type of the workflow definition.
   * @param initialState The initial state of the workflow.
   * @param errorState The generic error state of the workflow.
   */
  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST", justification = "findbugs does not understand that S extends both WorkflowState end Enum")
  protected WorkflowDefinition(String type, S initialState, S errorState) {
    this(type, initialState, errorState, new WorkflowSettings.Builder().build());
  }

  /**
   * Create a workflow definition with customized workflow settings.
   * @param type The type of the workflow.
   * @param initialState The initial state of the workflow.
   * @param errorState The generic error state of the workflow.
   * @param settings The workflow settings.
   */
  @SuppressWarnings("unchecked")
  protected WorkflowDefinition(String type, S initialState, S errorState, WorkflowSettings settings) {
    super(type, initialState, errorState, settings);
    this.allStates = EnumSet.allOf((Class<S>) initialState.getClass());
    requireEnumValuesHaveMatchingMethod();
  }

  @SuppressFBWarnings(value="BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "findbugs does not understand that S extends both WorkflowState end Enum")
  private void requireEnumValuesHaveMatchingMethod() {
    for (S state : allStates) {
      requireStateMethodExists(state);
    }
  }

  /**
   * Return all states of the workflow.
   * @return Set of workflow states.
   */
  @Override
  public Set<S> getStates() {
    return allStates;
  }
}
