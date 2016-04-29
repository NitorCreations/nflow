package com.nitorcreations.nflow.engine.processing;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

public abstract class AbstractWorkflowProcessingFactory {

  public abstract WorkflowProcessingInstance createInstance(WorkflowInstance workflowInstance);

  /**
   * Return true if this class is able to process workflowInstance.
   * If this return
   */
  public abstract boolean appliesTo(WorkflowInstance workflowInstance);
}
