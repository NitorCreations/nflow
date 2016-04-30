package com.nitorcreations.nflow.engine.processing;

import java.util.List;

public interface WorkflowProcessingDefinition {
  String getName();
  String getDescription();

  /**
   * State for new workflow if no state is given.
   * TODO is this actually needed at all?
   */
  WorkflowProcessingState getDefaultInitialState();

  /**
   * Generic error state where to go if WorkflowProcessingState doesn't define error state.
   * @return
   */
  WorkflowProcessingState getGenericErrorState();

  /**
   * List of all states.
   * @return
   */
  List<WorkflowProcessingState> getStates();

  WorkflowProcessingState getState(String stateName);

  WorkflowProcessingSettings getSettings();
}
