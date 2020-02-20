package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static io.nflow.engine.workflow.curated.CronWorkflow.VAR_SCHEDULE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.VAR_MAINTENANCE_CONFIGURATION;
import static org.joda.time.Period.days;
import static org.joda.time.Period.years;

@Component
public class MaintenanceWorkflowStarter {
  public static final String MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID = "default";

  protected final WorkflowInstanceService instanceService;
  protected final boolean insertOnStartup;

  public MaintenanceWorkflowStarter(Environment env, WorkflowInstanceService instanceService) {
    this.instanceService = instanceService;
    this.insertOnStartup = env.getProperty("nflow.maintenance.insertWorkflowIfMissing", Boolean.class, false);
  }

  protected String getInitialCronSchedule() {
    return "4 4 4 * * *";
  }

  protected MaintenanceConfiguration getInitialConfiguration() {
    return new MaintenanceConfiguration.Builder()
            .withDeleteArchivedWorkflows().setOlderThanPeriod(years(1)).done()
            .withArchiveWorkflows().setOlderThanPeriod(days(10)).done()
            .build();
  }

  @EventListener(ContextStartedEvent.class)
  public void start() {
    if (insertOnStartup) {
      instanceService.insertWorkflowInstance(new WorkflowInstance.Builder()
              .setType(MAINTENANCE_WORKFLOW_TYPE)
              .putStateVariable(VAR_SCHEDULE, getInitialCronSchedule())
              .putStateVariable(VAR_MAINTENANCE_CONFIGURATION, getInitialConfiguration())
              .setExternalId(MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID)
              .build());
    }
  }
}
