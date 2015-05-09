package com.nitorcreations.nflow.engine.internal.executor;

import javax.inject.Inject;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final Environment env;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    this.env = env;
  }

  public WorkflowStateProcessor createProcessor(int instanceId) {
    return new WorkflowStateProcessor(instanceId, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
            workflowInstancePreProcessor, env, listeners);
  }

}
