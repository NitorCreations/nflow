package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;

/**
 * Workflow that cleans up workflow instances periodically.
 */
@Component
public class MaintenanceWorkflow extends CronWorkflow {
  public static final String MAINTENANCE_WORKFLOW_TYPE = "nFlowMaintenance";
  public static final String VAR_MAINTENANCE_CONFIGURATION = "config";

  @Inject
  MaintenanceService maintenanceService;

  public MaintenanceWorkflow() {
    super(MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Clean up workflow instances periodically.");
  }

  public NextAction doWork(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_MAINTENANCE_CONFIGURATION, readOnly = true) MaintenanceConfiguration conf) {
    MaintenanceResults results = maintenanceService.cleanupWorkflows(conf);
    StringBuilder sb = new StringBuilder(64);
    sb.append("Maintenance:");
    add(sb, "Archived", results.archivedWorkflows);
    add(sb, "Deleted", results.deletedWorkflows);
    add(sb, "Deleted archived", results.deletedArchivedWorkflows);
    return moveToState(schedule, sb.toString());
  }

  private void add(StringBuilder sb, String type, int count) {
    if (count > 0) {
      sb.append(' ').append(type).append(":").append(count);
    }
  }
}
