package com.nitorcreations.nflow.engine;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;

@Component
public class WorkflowExecutorFactory {

  private final RepositoryService repository;

  @Inject
  public WorkflowExecutorFactory(RepositoryService repository) {
    this.repository = repository;
  }

  public WorkflowExecutor createExecutor(int instanceId) {
    return new WorkflowExecutor(instanceId, repository);
  }
}
