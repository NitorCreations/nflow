package com.nitorcreations.nflow.engine.processing;

import java.util.List;

public interface WorkflowProcessingDefinition {
  String getName();
  String getDescription();

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
}
