package com.nitorcreations.nflow.engine.processing;

import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

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
  WorkflowProcessingState getErrorState();

  /**
   * Max number of consecutive retries in state
   * @return max number of retries.
   */
  int getRetryCountLimit();

  /**
   * Warn if execution time exeeds this value.
   * @return limit in seconds.
   */
  int getExecutionTimeoutWarningLimit();
}
