package io.nflow.engine.guice;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.nflow.engine.internal.executor.WorkflowLifecycle;
import io.nflow.engine.internal.workflow.MaintenanceWorkflowStarter;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

@Singleton
public class NflowController {
  private final WorkflowLifecycle lifecycle;
  private final WorkflowDefinitionService workflowDefinitionService;
  private final MaintenanceWorkflowStarter maintenanceWorkflowStarter;
  private final Set<WorkflowDefinition> workflows;

  @Inject
  public NflowController(WorkflowLifecycle lifecycle, WorkflowDefinitionService workflowDefinitionService,
      MaintenanceWorkflowStarter maintenanceWorkflowStarter, Set<WorkflowDefinition> workflowDefinitions) {
    this.lifecycle = lifecycle;
    this.workflowDefinitionService = workflowDefinitionService;
    this.maintenanceWorkflowStarter = maintenanceWorkflowStarter;
    this.workflows = workflowDefinitions;
  }

  public void start() {
    try {
      workflows.forEach(workflowDefinitionService::addWorkflowDefinition);
      maintenanceWorkflowStarter.start();
    } catch (Exception e) {
      throw new RuntimeException("Failed to register workflows", e);
    }
    lifecycle.start();
  }

  public void stop() {
    lifecycle.stop();
  }
}
