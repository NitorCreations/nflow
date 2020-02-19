package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;

/**
 * Workflow that does nflow maintenance periodically.
 */
@Component
public class MaintenanceWorkflow extends CronWorkflow {
  public static final String MAINTENANCE_WORKFLOW_TYPE = "maintenance";
  public static final String MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID = "default";
  public static final String VAR_MAINTENANCE_CONFIGURATION = "conf";

  @Inject
  MaintenanceService maintenanceService;

  public MaintenanceWorkflow() {
    super(MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Do nFlow maintenance periodically.");
  }

  public NextAction doWork(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_MAINTENANCE_CONFIGURATION, readOnly = true) MaintenanceConfiguration conf) {
    maintenanceService.cleanupWorkflows(conf);
    return moveToState(schedule, "work done");
  }

}
