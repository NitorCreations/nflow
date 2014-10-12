package com.nitorcreations.nflow.engine.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;

/**
 * Service for managing workflow executors.
 */
@Component
public class WorkflowExecutorService {

  private final ExecutorDao executorDao;

  @Inject
  public WorkflowExecutorService(ExecutorDao executorDao) {
    this.executorDao = executorDao;
  }

  /**
   * Return all workflow executors of this executor group.
   * @return The workflow executors.
   */
  public List<WorkflowExecutor> getWorkflowExecutors() {
    return executorDao.getExecutors();
  }
}
