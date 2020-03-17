package io.nflow.engine.workflow.definition;

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
  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST", justification = "findbugs does not understand that S extends both WorkflowState end Enum")
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
   * {@inheritDoc}
   */
  @Override
  public Set<S> getStates() {
    return allStates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public final boolean isRetryable(Throwable throwable, WorkflowState state) {
    return isRetryable(throwable, (S) state);
  }

  /**
   * Return true if the same state can be retried after throwing an exception or false if the workflow instance should move
   * directly to a failure state. By default calls {@link AbstractWorkflowDefinition#isRetryable(Throwable, WorkflowState)},
   * override for custom logic.
   *
   * @param throwable
   *          The thrown exception.
   * @param state
   *          The state that was processed.
   * @return True if the exception is retryable in the state, false to move to a failure state.
   */
  protected boolean isRetryable(Throwable throwable, S state) {
    return super.isRetryable(throwable, state);
  }
}
