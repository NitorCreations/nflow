package io.nflow.engine.service;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;

/**
 * Register workflow definitions defined as Spring beans.
 */
@Component
public class WorkflowDefinitionSpringBeanScanner {

  @Inject
  public WorkflowDefinitionSpringBeanScanner(WorkflowDefinitionService workflowDefinitionService,
      Collection<AbstractWorkflowDefinition> workflowDefinitions) {
    workflowDefinitions.forEach(workflowDefinitionService::addWorkflowDefinition);
  }
}
