package io.nflow.engine.workflow.instance;

import io.nflow.engine.internal.workflow.ObjectStringMapper;

/**
 * Factory to create workflow instances.
 */
public class WorkflowInstanceFactory {

  private final ObjectStringMapper objectMapper;

  /**
   * Create a workflow instance factory.
   *
   * @param objectMapper
   *          The object mapper to be used to serialize and deserialize the
   *          state variables.
   */
  public WorkflowInstanceFactory(ObjectStringMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Return a new workflow instance builder.
   * @return Workflow instance builder.
   */
  public WorkflowInstance.Builder newWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder(objectMapper);
  }
}
