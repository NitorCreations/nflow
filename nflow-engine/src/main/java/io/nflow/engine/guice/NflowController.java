package io.nflow.engine.guice;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.executor.WorkflowLifecycle;
import io.nflow.engine.internal.workflow.MaintenanceWorkflowStarter;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

public class NflowController {
  private final WorkflowLifecycle lifecycle;
  private final WorkflowDefinitionService workflowDefinitionService;
  private final MaintenanceWorkflowStarter maintenanceWorkflowStarter;
  private final Set<WorkflowDefinition> workflows;

  public NflowController(WorkflowLifecycle lifecycle, WorkflowDefinitionService workflowDefinitionService,
      MaintenanceWorkflowStarter maintenanceWorkflowStarter, Set<WorkflowDefinition> workflowDefinitions) {
    this.lifecycle = lifecycle;
    this.workflowDefinitionService = workflowDefinitionService;
    this.maintenanceWorkflowStarter = maintenanceWorkflowStarter;
    this.workflows = workflowDefinitions;
  }

  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "RuntimeException message is ok")
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
