package com.nitorcreations.nflow.engine.workflow.instance;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;

/**
 * Factory to create workflow instances.
 */
@Component
public class WorkflowInstanceFactory {

  private final ObjectStringMapper objectMapper;

  /**
   * Create a workflow instance factory.
   *
   * @param objectMapper
   *          The object mapper to be used to serialize and deserialize the
   *          state variables.
   */
  @Inject
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
