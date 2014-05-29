package com.nitorcreations.nflow.engine;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;

@Component
public class WorkflowExecutorFactory {

  private final RepositoryService repository;
  private final ObjectStringMapper objectMapper;
  private final WorkflowExecutorListener[] listeners;

  @Inject
  public WorkflowExecutorFactory(RepositoryService repository, ObjectStringMapper objectMapper, Provider<List<WorkflowExecutorListener>> listeners) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    List<WorkflowExecutorListener> l = listeners.get();
    this.listeners = l != null ? l.toArray(new WorkflowExecutorListener[l.size()]) : new WorkflowExecutorListener[0];
  }

  public WorkflowExecutor createExecutor(int instanceId) {
    return new WorkflowExecutor(instanceId, objectMapper, repository, listeners);
  }
}
