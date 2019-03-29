package io.nflow.engine.processing.nflow;

import io.nflow.engine.processing.WorkflowProcessingDefinition;
import io.nflow.engine.processing.WorkflowProcessingSettings;
import io.nflow.engine.processing.WorkflowProcessingState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts old-school nflow workflow style definition to a WorkflowProcessingDefinition.
 */
public class StandardNfloWorkflowProcessingDefinition implements WorkflowProcessingDefinition
{
  private final AbstractWorkflowDefinition<? extends WorkflowState> definition;
  private final WorkflowProcessingSettings settings;

  public StandardNfloWorkflowProcessingDefinition(AbstractWorkflowDefinition<? extends WorkflowState> definition,
                                                  WorkflowProcessingSettings settings) {
    this.definition = definition;
    this.settings = settings;
  }

  @Override
  public String getName() {
    return definition.getName();
  }

  @Override
  public String getDescription() {
    return definition.getDescription();
  }

  /**
   * State for new workflow if no state is given.
   */
  @Override
  public WorkflowProcessingState getDefaultInitialState() {
    // TODO
    return getState(definition.getInitialState());
  }

  @Override
  public WorkflowProcessingState getGenericErrorState() {
    // TODO
    return getState(definition.getErrorState());
  }

  @Override
  public List<WorkflowProcessingState> getStates() {
    List<WorkflowProcessingState> processingStates = new ArrayList<>();
    for (WorkflowState state : definition.getStates()) {
      WorkflowProcessingState processingState = new StandardNflowWorkflowProcessingState(definition, state);
      processingStates.add(processingState);
    }
    return processingStates;
  }

  @Override
  public WorkflowProcessingState getState(String stateName) {
    // TODO Lookup from hashmap
    for (WorkflowProcessingState state : getStates()) {
      if(state.getName().equals(stateName)) {
        return state;
      }
    }
    throw new IllegalArgumentException("unknown state " + stateName);
  }

  @Override
  public WorkflowProcessingSettings getSettings() {
    return settings;
  }

  public WorkflowProcessingState getState(WorkflowState state) {
    return getState(state.name());
  }

}
