package com.nitorcreations.nflow.engine.internal.executor;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

@Component
public class WorkflowExecutorFactory {

  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];

  @Inject
  public WorkflowExecutorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
  }

  @Autowired(required = false)
  public WorkflowExecutorFactory setListeners(List<WorkflowExecutorListener> listeners) {
    this.listeners = listeners.toArray(new WorkflowExecutorListener[listeners.size()]);
    return this;
  }

  public WorkflowExecutor createExecutor(int instanceId) {
    return new WorkflowExecutor(instanceId, objectMapper, workflowDefinitions, workflowInstances, listeners);
  }

}
