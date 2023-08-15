package io.nflow.engine.service;

import java.util.Collection;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.WorkflowDefinition;

/**
 * Register workflow definitions defined as Spring beans.
 */
@Component
public class WorkflowDefinitionSpringBeanScanner {

  @Inject
  public WorkflowDefinitionSpringBeanScanner(WorkflowDefinitionService workflowDefinitionService,
      Collection<WorkflowDefinition> workflowDefinitions) {
    workflowDefinitions.forEach(workflowDefinitionService::addWorkflowDefinition);
  }
}
