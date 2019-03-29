package io.nflow.engine.processing;

import io.nflow.engine.workflow.definition.WorkflowStateType;

import java.util.List;

public interface WorkflowProcessingState {
  /**
   * Type of state
   * @return
   */
  WorkflowStateType getType();

  /**
   * Name of state. Must be unique within same workflow.
   * @return
   */
  String getName();

  /**
   * Human readable description of the state.
   * @return
   */
  String getDescription();

  /**
   * @return List of possible next states.
   */
  List<WorkflowProcessingState> possileNextStates();

  /**
   * @return Error state where to go in case of error.
   */
  WorkflowProcessingState getFailureState();

  /**
   * Max number of consecutive retries in state
   * @return max number of retries.
   */
  int getMaxRetries();

}
