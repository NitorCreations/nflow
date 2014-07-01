package com.nitorcreations.nflow.engine.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;

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
