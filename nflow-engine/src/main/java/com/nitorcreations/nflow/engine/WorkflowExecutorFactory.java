package com.nitorcreations.nflow.engine;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;

@Component
public class WorkflowExecutorFactory {

  private static final Logger logger = getLogger(WorkflowExecutorFactory.class);

  private final RepositoryService repository;
  
  @Inject
  public WorkflowExecutorFactory(RepositoryService repository) {
    this.repository = repository;
  }
   
  public WorkflowExecutor createExecutor(
      Integer instanceId) {
    return new WorkflowExecutor(instanceId, repository);
  }

}
