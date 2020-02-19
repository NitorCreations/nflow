package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.Builder;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static java.lang.Integer.MAX_VALUE;
import static org.joda.time.DateTime.now;
import static org.joda.time.Period.days;
import static org.joda.time.Period.years;

/**
 * Workflow that does nflow maintenance periodically.
 */
@Component
public class MaintenanceWorkflow extends CronWorkflow {
  public static final String MAINTENANCE_WORKFLOW_TYPE = "maintenance";
  public static final String MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID = "default";
  public static final String VAR_MAINTENANCE_CONFIGURATON = "conf";

  @Inject
  MaintenanceService maintenanceService;

  public MaintenanceWorkflow() {
    super(MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Does nflow maintenance periodically.");
  }

  public NextAction doWork(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_MAINTENANCE_CONFIGURATON, readOnly = true) MaintenanceConfiguration conf) {
    maintenanceService.cleanupWorkflows(conf);
    return moveToState(schedule, "work done");
  }

  @Service
  static class MaintenanceConfigurationService {
    private final MaintenanceConfiguration defaultConfiguration;
    private final WorkflowInstanceService instanceService;
    private final boolean insertOnStartup;

    public MaintenanceConfigurationService(Environment env, WorkflowInstanceService instanceService) {
      defaultConfiguration = new Builder()
              .withDeleteArchivedWorkflows().setOlderThanPeriod(years(1)).done()
              .withArchiveWorkflows().setOlderThanPeriod(days(2)).done()
              .build();
      this.instanceService = instanceService;
      this.insertOnStartup = env.getProperty("nflow.maintenance.start", Boolean.class, false);
    }

    protected MaintenanceConfiguration getDefaultConfiguration() {
      return defaultConfiguration;
    }

    @EventListener(ContextStartedEvent.class)
    public void start() {
      if (insertOnStartup) {
        instanceService.insertWorkflowInstance(new WorkflowInstance.Builder()
                .setType(MAINTENANCE_WORKFLOW_TYPE)
                .putStateVariable(VAR_SCHEDULE, "4 4 4 * * *")
                .putStateVariable(VAR_MAINTENANCE_CONFIGURATON, getDefaultConfiguration())
                .setExternalId(MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID)
                .build());
      }
    }
  }
}
