package io.nflow.engine.workflow.curated;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowSettings;

/**
 * Clean up workflow instances periodically.
 */
@Component
public class MaintenanceWorkflow extends CronWorkflow {
  public static final String MAINTENANCE_WORKFLOW_TYPE = "nFlowMaintenance";
  public static final String VAR_MAINTENANCE_CONFIGURATION = "config";

  @Inject
  private MaintenanceService maintenanceService;

  /**
   * Create maintenance workflow definition.
   */
  public MaintenanceWorkflow() {
    super(MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Clean up workflow instances periodically.");
  }

  /**
   * Extend maintenance workflow definition with customer workflow settings.
   *
   * @param type
   *          The type of the workflow definition.
   * @param settings
   *          The workflow settings.
   */
  protected MaintenanceWorkflow(String type, WorkflowSettings settings) {
    super(type, settings);
    setDescription("Clean up workflow instances periodically.");
  }

  /**
   * Clean up old workflow instances.
   *
   * @param execution
   *          State execution context.
   * @param conf
   *          The maintenance configuration.
   * @return The action to go schedule state.
   */
  public NextAction doWork(StateExecution execution,
      @StateVar(value = VAR_MAINTENANCE_CONFIGURATION, readOnly = true) MaintenanceConfiguration conf) {
    MaintenanceResults results = maintenanceService.cleanupWorkflows(conf);
    StringBuilder sb = new StringBuilder(64);
    add(sb, "Archived", results.archivedWorkflows);
    add(sb, "Deleted", results.deletedWorkflows);
    add(sb, "Deleted archived", results.deletedArchivedWorkflows);
    if (sb.length() == 0) {
      sb.append("No actions");
    }
    return moveToState(schedule, sb.toString());
  }

  private void add(StringBuilder sb, String type, int count) {
    if (count > 0) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(type).append(":").append(count);
    }
  }
}
