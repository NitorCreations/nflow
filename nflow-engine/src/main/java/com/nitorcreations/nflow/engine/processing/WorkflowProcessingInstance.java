package com.nitorcreations.nflow.engine.processing;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import org.joda.time.DateTime;

public interface WorkflowProcessingInstance {
  WorkflowProcessingDefinition getWorkflowDefinition();
  WorkflowProcessingState getCurrentState();

  /**
   * State processing happens here
   * @param stateExecution
   * @return
   */
  NextProcessingAction executeState(StateExecution stateExecution);

  /**
   * Current number of retries.
   * @return
   */
  int getRetryCount();

  /**
   * In case of retry, this will show next when state execution is attempted for the next time.
   * If you return null, you'll get binary backoff algorithm.
   * @return
   */
  DateTime nextRetryTime();
}
