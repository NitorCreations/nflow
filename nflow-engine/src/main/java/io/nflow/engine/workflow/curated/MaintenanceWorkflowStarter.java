package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static org.joda.time.Period.days;
import static org.joda.time.Period.years;

@Component
public class MaintenanceWorkflowStarter {
  protected final WorkflowInstanceService instanceService;
  protected final boolean insertOnStartup;

  public MaintenanceWorkflowStarter(Environment env, WorkflowInstanceService instanceService) {
    this.instanceService = instanceService;
    this.insertOnStartup = env.getProperty("nflow.maintenance.start", Boolean.class, false);
  }

  protected String getDefaultCronSchedule() {
    return "4 4 4 * * *";
  }

  protected MaintenanceConfiguration getDefaultConfiguration() {
    return new MaintenanceConfiguration.Builder()
            .withDeleteArchivedWorkflows().setOlderThanPeriod(years(1)).done()
            .withArchiveWorkflows().setOlderThanPeriod(days(2)).done()
            .build();
  }

  @EventListener(ContextStartedEvent.class)
  public void start() {
    if (insertOnStartup) {
      instanceService.insertWorkflowInstance(new WorkflowInstance.Builder()
              .setType(MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE)
              .putStateVariable(CronWorkflow.VAR_SCHEDULE, getDefaultCronSchedule())
              .putStateVariable(MaintenanceWorkflow.VAR_MAINTENANCE_CONFIGURATION, getDefaultConfiguration())
              .setExternalId(MaintenanceWorkflow.MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID)
              .build());
    }
  }
}
