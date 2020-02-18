package io.nflow.tests.demo.workflow;

import static org.joda.time.DateTime.now;
import static org.joda.time.Period.days;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.CronWorkflow;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;

/**
 * Example cron workflow that cleans up old workflow instances that are older than one day every hour.
 */
@Component
public class MaintenanceCronWorkflow extends CronWorkflow {

  public static final String TYPE = "maintenanceCronWorkflow";
  private final MaintenanceService maintenanceService;
  private final MaintenanceConfiguration configuration;

  @Inject
  public MaintenanceCronWorkflow(MaintenanceService maintenanceService) {
    super(TYPE, new WorkflowSettings.Builder().build());
    setDescription("Example cron workflow that cleans up old workflow instances that are older than one day every hour.");
    this.maintenanceService = maintenanceService;
    configuration = new MaintenanceConfiguration.Builder()
        .setDeleteWorkflows(new ConfigurationItem.Builder().setOlderThanPeriod(days(1)).build()).build();
  }

  @Override
  protected boolean executeTaskImpl(StateExecution execution) {
    maintenanceService.cleanupWorkflows(configuration);
    return true;
  }

  @Override
  protected DateTime getNextExecutionTime() {
    return now().plusHours(1);
  }

}
