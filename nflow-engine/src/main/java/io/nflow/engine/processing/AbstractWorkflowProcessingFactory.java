package io.nflow.engine.processing;

import io.nflow.engine.workflow.instance.WorkflowInstance;

// TODO should this be interface?
public abstract class AbstractWorkflowProcessingFactory {

  public abstract WorkflowProcessingInstance createInstance(WorkflowInstance workflowInstance);

  /**
   * Return true if this class is able to process workflowInstance.
   * If this returns true, then createInstance() can be called on the workflowInstance.
   */
  public abstract boolean appliesTo(WorkflowInstance workflowInstance);
}
