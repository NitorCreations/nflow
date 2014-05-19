package com.nitorcreations.nflow.engine;

import static java.util.Collections.emptyList;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;

@Component
public class WorkflowExecutorFactory {

  private final RepositoryService repository;
  private List<WorkflowExecutorListener> listeners = emptyList();

  @Inject
  public WorkflowExecutorFactory(RepositoryService repository) {
    this.repository = repository;
  }

  @Autowired(required = false)
  public WorkflowExecutorFactory setListeners(List<WorkflowExecutorListener> listeners) {
    this.listeners = listeners;
    return this;
  }

  public WorkflowExecutor createExecutor(int instanceId) {
    return new WorkflowExecutor(instanceId, repository, listeners.toArray(new WorkflowExecutorListener[listeners.size()]));
  }
}
