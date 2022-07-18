package io.nflow.engine.workflow.curated;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.ExecutorMaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowSettings;

/**
 * Clean up workflow executors periodically.
 */
@Component
public class ExecutorMaintenanceWorkflow extends CronWorkflow {
  public static final String EXECUTOR_MAINTENANCE_WORKFLOW_TYPE = "nFlowExecutorMaintenance";
  public static final String VAR_MAINTENANCE_CONFIGURATION = "config";

  @Inject
  private MaintenanceService maintenanceService;

  /**
   * Create executor maintenance workflow definition.
   */
  public ExecutorMaintenanceWorkflow() {
    super(EXECUTOR_MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Clean up workflow executors periodically.");
  }

  /**
   * Extend executor maintenance workflow definition with custom workflow settings.
   *
   * @param type
   *          The type of the workflow definition.
   * @param settings
   *          The workflow settings.
   */
  protected ExecutorMaintenanceWorkflow(String type, WorkflowSettings settings) {
    super(type, settings);
    setDescription("Clean up workflow executors periodically.");
  }

  /**
   * Clean up old workflow executors.
   *
   * @param execution
   *          State execution context.
   * @param conf
   *          The maintenance configuration.
   * @return The action to go schedule state.
   */
  public NextAction doWork(StateExecution execution,
      @StateVar(value = VAR_MAINTENANCE_CONFIGURATION, readOnly = true) ExecutorMaintenanceConfiguration conf) {
    int deleted = maintenanceService.cleanupExecutors(conf);
    String reason = deleted > 0 ? "Deleted " + deleted + " executors" : "No executors deleted";
    return moveToState(SCHEDULE, reason);
  }
}
