package com.nitorcreations.nflow.engine.internal.executor;

import javax.inject.Inject;

import org.springframework.beans.factory.BeanFactory;
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
  private final Environment env;
  private final BeanFactory beanFactory;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao, Environment env, BeanFactory beanFactory) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
    this.env = env;
    this.beanFactory = beanFactory;
  }

  public WorkflowStateProcessor createProcessor(int instanceId) {
    return new WorkflowStateProcessor(instanceId, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        env, beanFactory, listeners);
  }

}
