package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.MAIN;
import static java.lang.Math.max;
import static java.lang.String.format;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration.ConfigurationItem;

/**
 * Service for deleting and archiving old workflow instances from nflow-tables and nflow_archive-tables.
 */
@Named
public class MaintenanceService {
  private static final Logger log = getLogger(MaintenanceService.class);

  private final MaintenanceDao maintenanceDao;

  @Inject
  public MaintenanceService(MaintenanceDao maintenanceDao) {
    this.maintenanceDao = maintenanceDao;
  }

  /**
   * Archive and delete old (whose modified time is earlier than <code>olderThanPeriod</code> parameter) and passive (that do not
   * have <code>nextActivation</code>) workflows. Copies workflow instances, workflow instance actions and state variables to
   * corresponding archive tables and removes them from production tables.
   *
   * @param configuration
   *          Cleanup actions to be executed and parameters for the actions.
   * @return Object describing the number of workflows acted on.
   */
  @SuppressFBWarnings(value = "BAS_BLOATED_ASSIGNMENT_SCOPE", justification = "periodicLogger is defined in correct scope")
  public MaintenanceResults cleanupWorkflows(MaintenanceConfiguration configuration) {
    if (configuration.archiveWorkflows != null || configuration.deleteArchivedWorkflows != null) {
      maintenanceDao.ensureValidArchiveTablesExist();
    }

    MaintenanceResults res = new MaintenanceResults();
    if (configuration.deleteArchivedWorkflows != null) {
      Supplier<List<Long>> source = getIdQuery(ARCHIVE, configuration.deleteArchivedWorkflows);
      res.deletedArchivedWorkflows = doAction("Deleting archived workflows", configuration.deleteArchivedWorkflows, source,
          idList -> maintenanceDao.deleteWorkflows(ARCHIVE, idList));
    }
    if (configuration.archiveWorkflows != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration.archiveWorkflows);
      res.archivedWorkflows = doAction("Archiving workflows", configuration.archiveWorkflows, source,
          maintenanceDao::archiveWorkflows);
    }
    if (configuration.deleteWorkflows != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration.deleteWorkflows);
      res.deletedWorkflows = doAction("Deleting workflows", configuration.deleteWorkflows, source,
          idList -> maintenanceDao.deleteWorkflows(MAIN, idList));
    }
    if (configuration.deleteStates != null) {
      // TODO
    }
    return res;
  }

  private Supplier<List<Long>> getIdQuery(TablePrefix table, ConfigurationItem configuration) {
    DateTime olderThan = now().minus(configuration.olderThanPeriod);
    return () -> maintenanceDao.listOldWorkflows(table, olderThan, configuration.batchSize);
  }

  private int doAction(String type, ConfigurationItem configuration, Supplier<List<Long>> getActionables,
      Function<List<Long>, Integer> doAction) {
    log.info("{} older than {}, in batches of {}.", type, configuration.olderThanPeriod, configuration.batchSize);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    PeriodicLogger periodicLogger = new PeriodicLogger(log, 60);
    int totalWorkflows = 0;
    do {
      List<Long> workflowIds = getActionables.get();
      if (workflowIds.isEmpty()) {
        break;
      }
      workflowIds.sort(null);
      int workflows = doAction.apply(workflowIds);
      totalWorkflows += workflows;

      double timeDiff = max(stopWatch.getTime() / 1000.0, 0.000001);
      String status = format("%s. %s workflows, %.1f workflows / second.", type, workflows, totalWorkflows / timeDiff);
      if (log.isDebugEnabled()) {
        log.debug(status + " Workflow ids: {}.", workflowIds);
      }
      periodicLogger.info(status);
    } while (true);

    log.info("{} finished. Operated on {} workflows in {} seconds.", type, totalWorkflows, stopWatch.getTime() / 1000);
    return totalWorkflows;
  }

  public static class MaintenanceConfiguration {
    public final ConfigurationItem deleteArchivedWorkflows;
    public final ConfigurationItem archiveWorkflows;
    public final ConfigurationItem deleteWorkflows;
    public final ConfigurationItem deleteStates;

    MaintenanceConfiguration(ConfigurationItem deleteArchivedWorkflows, ConfigurationItem archiveWorkflows,
        ConfigurationItem deleteWorkflows, ConfigurationItem deleteStates) {
      this.deleteArchivedWorkflows = deleteArchivedWorkflows;
      this.archiveWorkflows = archiveWorkflows;
      this.deleteWorkflows = deleteWorkflows;
      this.deleteStates = deleteStates;
    }

    public static class Builder {
      private ConfigurationItem deleteArchivedWorkflows;
      private ConfigurationItem archiveWorkflows;
      private ConfigurationItem deleteWorkflows;
      private ConfigurationItem deleteStates;

      public Builder setDeleteArchivedWorkflows(ConfigurationItem deleteArchivedWorkflows) {
        this.deleteArchivedWorkflows = deleteArchivedWorkflows;
        return this;
      }

      public Builder setArchiveWorkflows(ConfigurationItem archiveWorkflows) {
        this.archiveWorkflows = archiveWorkflows;
        return this;
      }

      public Builder setDeleteWorkflows(ConfigurationItem deleteWorkflows) {
        this.deleteWorkflows = deleteWorkflows;
        return this;
      }

      public Builder setDeleteStates(ConfigurationItem deleteStates) {
        this.deleteStates = deleteStates;
        return this;
      }

      public MaintenanceConfiguration build() {
        return new MaintenanceConfiguration(deleteArchivedWorkflows, archiveWorkflows, deleteWorkflows, deleteStates);
      }
    }

    public static class ConfigurationItem {

      public final ReadablePeriod olderThanPeriod;
      public final int batchSize;

      public ConfigurationItem(ReadablePeriod olderThanPeriod, Integer batchSize) {
        this.olderThanPeriod = olderThanPeriod;
        this.batchSize = batchSize;
      }

      public static class Builder {
        private ReadablePeriod olderThanPeriod;
        private Integer batchSize = 1000;

        public Builder setOlderThanPeriod(ReadablePeriod olderThanPeriod) {
          this.olderThanPeriod = olderThanPeriod;
          return this;
        }

        /**
         * @param batchSize
         *          Number of workflows to operate on in single transaction. Typical value is 100-1000. This parameter mostly
         *          affects on performance.
         */
        public Builder setBatchSize(int batchSize) {
          this.batchSize = batchSize;
          return this;
        }

        public ConfigurationItem build() {
          Assert.isTrue(olderThanPeriod != null, "olderThanPeriod must not be null");
          Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
          return new ConfigurationItem(olderThanPeriod, batchSize);
        }
      }
    }
  }

  public static class MaintenanceResults {
    public int deletedArchivedWorkflows;
    public int archivedWorkflows;
    public int deletedWorkflows;
    public int deletedStates;
  }
}
