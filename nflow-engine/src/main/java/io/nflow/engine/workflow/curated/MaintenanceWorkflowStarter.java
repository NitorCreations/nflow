package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static io.nflow.engine.workflow.curated.CronWorkflow.VAR_SCHEDULE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.VAR_MAINTENANCE_CONFIGURATION;
import static java.util.Optional.ofNullable;

@Component
public class MaintenanceWorkflowStarter {
  public static final String MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID = "default";

  protected WorkflowInstanceService instanceService;
  protected boolean insertOnStartup;
  protected String initialCronSchedule;
  protected MaintenanceConfiguration initialConfiguration;

  public MaintenanceWorkflowStarter(Environment env, WorkflowInstanceService instanceService) {
    this.instanceService = instanceService;
    this.insertOnStartup = env.getRequiredProperty("nflow.maintenance.insertWorkflowIfMissing", Boolean.class);
    this.initialCronSchedule = env.getRequiredProperty("nflow.maintenance.initial.cron");
    MaintenanceConfiguration.Builder builder = new MaintenanceConfiguration.Builder();
    apply(env, "archive", builder::withArchiveWorkflows);
    apply(env, "delete", builder::withDeleteWorkflows);
    apply(env, "deleteArchived", builder::withDeleteArchivedWorkflows);
    this.initialConfiguration = builder.build();
  }

  private void apply(Environment env, String property, Supplier<ConfigurationItem.Builder> builderSupplier) {
    ofNullable(env.getProperty("nflow.maintenance.initial." + property + ".olderThan"))
            .map(StringUtils::trimToNull)
            .map(Period::parse)
            .ifPresent(period -> builderSupplier.get().setOlderThanPeriod(period).done());
  }

  @EventListener(ContextStartedEvent.class)
  public void start() {
    if (insertOnStartup) {
      instanceService.insertWorkflowInstance(new WorkflowInstance.Builder()
              .setType(MAINTENANCE_WORKFLOW_TYPE)
              .putStateVariable(VAR_SCHEDULE, initialCronSchedule)
              .putStateVariable(VAR_MAINTENANCE_CONFIGURATION, initialConfiguration)
              .setExternalId(MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID)
              .build());
    }
  }
}
