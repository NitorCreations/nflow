package com.nitorcreations.nflow.engine.domain;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;

@Component
public class WorkflowInstanceFactory {

  private final ObjectStringMapper objectMapper;

  @Inject
  public WorkflowInstanceFactory(ObjectStringMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public WorkflowInstance.Builder newWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder(objectMapper);
  }
}
