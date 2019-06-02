package io.nflow.engine.service;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

/**
 * Register workflow definitions defined as Spring beans.
 */
@Component
public class WorkflowDefinitionSpringBeanScanner {

  @Inject
  public WorkflowDefinitionSpringBeanScanner(WorkflowDefinitionService workflowDefinitionService,
      Collection<AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
    workflowDefinitions.forEach(workflowDefinitionService::addWorkflowDefinition);
  }

}
