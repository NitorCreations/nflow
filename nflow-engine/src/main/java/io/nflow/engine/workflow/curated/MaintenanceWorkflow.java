package io.nflow.engine.workflow.curated;

import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.Builder;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.Period;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Provider;

import java.util.HashSet;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
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
  Provider<DefaultMaintenanceConfigurationService> defaultConfiguration;

  @Inject
  MaintenanceService maintenanceService;

  public MaintenanceWorkflow() {
    super(MAINTENANCE_WORKFLOW_TYPE);
    setDescription("Does nflow maintenance periodically.");
  }

  protected void workImpl(StateExecution execution) {
    MaintenanceConfiguration conf = execution.getVariable(VAR_MAINTENANCE_CONFIGURATON, MaintenanceConfiguration.class, defaultConfiguration.get().get());
    maintenanceService.cleanupWorkflows(conf);
  }

  @Service
  static class DefaultMaintenanceConfigurationService implements SmartLifecycle {
    private final MaintenanceConfiguration defaultConfiguration;
    private final WorkflowInstanceService instanceService;
    private final String schedule;

    public DefaultMaintenanceConfigurationService(Environment env, WorkflowInstanceService instanceService) {
      defaultConfiguration = new Builder()
              .setDeleteArchivedWorkflows(getConf("delete_archive", env, years(1)))
              .setArchiveWorkflows(getConf("archive", env, days(2)))
              .setDeleteWorkflows(getConf("delete", env, days(7)))
              .build();
      this.instanceService = instanceService;
      String schedule = env.getProperty("maintenance.schedule");
      if (schedule != null && !schedule.isBlank()) {
        this.schedule = schedule.trim();
        new CronSequenceGenerator(this.schedule);
      } else {
        this.schedule = null;
      }
    }

    private ConfigurationItem getConf(String type, Environment env, Period defaultPeriod) {
      ConfigurationItem.Builder builder = new ConfigurationItem.Builder()
              .setOlderThanPeriod(env.getProperty("maintenance." + type + ".older_than", Period.class, defaultPeriod))
              .setWorkflowTypes(new HashSet<>(asList(env.getProperty("maintenance." + type + ".workflow_types", "").split(","))));
      Integer batchSize = env.getProperty("maintenance." + type + ".batch_size", Integer.class);
      if (batchSize != null) {
        builder.setBatchSize(batchSize);
      }
      return builder.build();
    }

    public MaintenanceConfiguration get() {
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
                .setStateVariables(singletonMap(VAR_SCHEDULE, schedule))
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
