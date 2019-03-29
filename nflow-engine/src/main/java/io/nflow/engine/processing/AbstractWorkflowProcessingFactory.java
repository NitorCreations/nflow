package io.nflow.engine.processing;

import io.nflow.engine.workflow.instance.WorkflowInstance;

public abstract class AbstractWorkflowProcessingFactory {

  public abstract WorkflowProcessingInstance createInstance(WorkflowInstance workflowInstance);

  /**
   * Return true if this class is able to process workflowInstance.
   * If this return
   */
  public abstract boolean appliesTo(WorkflowInstance workflowInstance);
}
