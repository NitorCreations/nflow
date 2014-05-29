package com.nitorcreations.nflow.engine;

import static java.util.Collections.emptyList;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;

@Component
public class WorkflowExecutorFactory {

  private final RepositoryService repository;
  private final ObjectStringMapper objectMapper;
  private List<WorkflowExecutorListener> listeners = emptyList();

  @Inject
  public WorkflowExecutorFactory(RepositoryService repository, ObjectStringMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Autowired(required = false)
  public WorkflowExecutorFactory setListeners(List<WorkflowExecutorListener> listeners) {
    this.listeners = listeners;
    return this;
  }

  public WorkflowExecutor createExecutor(int instanceId) {
    return new WorkflowExecutor(instanceId, objectMapper, repository, listeners.toArray(new WorkflowExecutorListener[listeners.size()]));
  }
}
