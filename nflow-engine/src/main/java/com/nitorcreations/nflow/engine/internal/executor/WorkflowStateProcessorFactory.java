package com.nitorcreations.nflow.engine.internal.executor;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

@Component
public class WorkflowStateProcessorFactory {
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  public WorkflowStateProcessor createProcessor(int instanceId) {
    return new WorkflowStateProcessor(instanceId, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        listeners);
  }

}
