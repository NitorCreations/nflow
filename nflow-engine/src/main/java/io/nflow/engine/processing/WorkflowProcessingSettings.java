package io.nflow.engine.processing;

public interface WorkflowProcessingSettings {
  // TODO WorkflowProcessingInstance.getMaxRetries()
  // WorkflowProcessingInstance.getNextRetryTime()
  // handle everything here. But these fields are part of e.g. REST API
  // TODO maybe replace settings with Map<String, String> ?

  /**
   * Minimum delay on execution retry after an error. Unit is milliseconds.
   */
  //int getMinErrorTransitionDelay();
  /**
   * Maximum delay on execution retry after an error. Unit is milliseconds.
   */
  //int getMaxErrorTransitionDelay();
  /**
   * Length of forced delay to break execution of a step that is considered to be busy looping. Unit is milliseconds.
   */
  //int getShortTransitionDelay();
  /**
   * Immediate transition delay.
   */
  //int getImmediateTransitionDelay();
  /**
   * Maximum retry attempts.
   */
  //int getMaxRetries();

}
