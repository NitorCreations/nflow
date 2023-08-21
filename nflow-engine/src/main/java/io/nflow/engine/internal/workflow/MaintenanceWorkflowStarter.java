package io.nflow.engine.internal.workflow;

import static io.nflow.engine.workflow.curated.CronWorkflow.VAR_SCHEDULE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.MAINTENANCE_WORKFLOW_TYPE;
import static io.nflow.engine.workflow.curated.MaintenanceWorkflow.VAR_MAINTENANCE_CONFIGURATION;
import static java.util.Optional.ofNullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@Component
public class MaintenanceWorkflowStarter {
  public static final String MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID = "default";

  protected final WorkflowInstanceFactory workflowInstanceFactory;
  protected final WorkflowInstanceService instanceService;
  protected final AtomicBoolean insertOnStartup = new AtomicBoolean();
  protected String initialCronSchedule;
  protected MaintenanceConfiguration initialConfiguration;

  @Inject
  public MaintenanceWorkflowStarter(Environment env, WorkflowInstanceService instanceService,
      WorkflowInstanceFactory workflowInstanceFactory) {
    this.workflowInstanceFactory = workflowInstanceFactory;
    this.instanceService = instanceService;
    this.insertOnStartup.set(env.getRequiredProperty("nflow.maintenance.insertWorkflowIfMissing", Boolean.class));
    this.initialCronSchedule = env.getRequiredProperty("nflow.maintenance.initial.cron");
    MaintenanceConfiguration.Builder builder = new MaintenanceConfiguration.Builder();
    apply(env, "archive", builder::withArchiveWorkflows);
    apply(env, "delete", builder::withDeleteWorkflows);
    apply(env, "deleteArchived", builder::withDeleteArchivedWorkflows);
    builder.withDeleteExpiredExecutorsOlderThan(
        Period.parse(env.getRequiredProperty("nflow.maintenance.initial.deleteExpiredExecutors.olderThan")));
    this.initialConfiguration = builder.build();
  }

  private void apply(Environment env, String property, Supplier<ConfigurationItem.Builder> builderSupplier) {
    ofNullable(env.getProperty("nflow.maintenance.initial." + property + ".olderThan"))
        .map(StringUtils::trimToNull)
        .map(Period::parse)
        .ifPresent(period -> builderSupplier.get().setOlderThanPeriod(period));
  }

  @EventListener(ContextRefreshedEvent.class)
  public void start() {
    if (insertOnStartup.compareAndSet(true, false)) {
      QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(MAINTENANCE_WORKFLOW_TYPE)
          .setExternalId(MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID).build();
      if (instanceService.listWorkflowInstances(query).isEmpty()) {
        instanceService.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
            .setType(MAINTENANCE_WORKFLOW_TYPE)
            .putStateVariable(VAR_SCHEDULE, initialCronSchedule)
            .putStateVariable(VAR_MAINTENANCE_CONFIGURATION, initialConfiguration)
            .setExternalId(MAINTENANCE_WORKFLOW_DEFAULT_EXTERNAL_ID)
            .build());
      }
    }
  }
}
