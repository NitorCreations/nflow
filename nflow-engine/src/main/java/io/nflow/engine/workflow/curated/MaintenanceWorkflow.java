package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.Builder;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.Period;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.HashSet;

import static io.nflow.engine.workflow.curated.CronWorkflow.State.schedule;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.joda.time.DateTime.now;
import static org.joda.time.Period.days;
import static org.joda.time.Period.years;

/**
 * Workflow that does nflow maintenance periodically.
 */
@Component
public class MaintenanceWorkflow extends CronWorkflow {
  public static final String MAINTENANCE_WORKFLOW_TYPE = "maintenance";
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
  static class MaintenanceConfigurationService implements SmartLifecycle {
    private final MaintenanceConfiguration defaultConfiguration;
    private final WorkflowInstanceService instanceService;
    private final String schedule;

    public MaintenanceConfigurationService(Environment env, WorkflowInstanceService instanceService) {
      Builder builder = new Builder();
      configure(builder.withDeleteArchivedWorkflows(), "delete_archive", env, years(1));
      configure(builder.withDeleteArchivedWorkflows(), "archive", env, days(2));
      configure(builder.withDeleteArchivedWorkflows(), "delete", env, days(7));
      defaultConfiguration = builder.build();
      this.instanceService = instanceService;
      if (env.getProperty("nflow.maintenance.auto_start", Boolean.class, false)) {
        this.schedule = env.getProperty("nflow.maintenance.cron", "4 4 4 * * *");
        new CronSequenceGenerator(this.schedule);
      } else {
        this.schedule = null;
      }
    }

    private void configure(ConfigurationItem.Builder builder, String type, Environment env, Period defaultPeriod) {
      builder
          .setOlderThanPeriod(ofNullable(env.getProperty("nflow.maintenance." + type + ".older_than", String.class)).map(Period::parse).orElse(defaultPeriod))
          .setWorkflowTypes(new HashSet<>(asList(env.getProperty("nflow.maintenance." + type + ".workflow_types", "").split(","))));
      Integer batchSize = env.getProperty("nflow.maintenance." + type + ".batch_size", Integer.class);
      if (batchSize != null) {
        builder.setBatchSize(batchSize);
      }
      builder.done();
    }

    public MaintenanceConfiguration getDefaultConfiguration() {
      return defaultConfiguration;
    }

    @Override
    public int getPhase() {
      return MAX_VALUE;
    }

    @Override
    public void start() {
      if (schedule != null) {
        instanceService.insertWorkflowInstance(new WorkflowInstance.Builder()
                .setType(MAINTENANCE_WORKFLOW_TYPE)
                .putStateVariable(VAR_SCHEDULE, schedule)
                .putStateVariable(VAR_MAINTENANCE_CONFIGURATON, getDefaultConfiguration())
                .setExternalId("default")
                .setNextActivation(now())
                .build());
      }
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
      return false;
    }
  }
}
