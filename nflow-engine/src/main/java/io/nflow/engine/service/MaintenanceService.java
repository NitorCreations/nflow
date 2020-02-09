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
   * Archive and delete old workflows. (whose modified time is earlier than <code>olderThan</code> parameter) and passive (that do not have
   * <code>nextActivation</code>) workflows. Copies workflow instances, workflow instance actions and state variables to
   * corresponding archive tables and removes them from production tables.
   *
   * @param configuration Passive workflow instances whose modified time is before this will be archived.
   * @return Object describing the number of workflows acted on.
   */
  @SuppressFBWarnings(value = "BAS_BLOATED_ASSIGNMENT_SCOPE", justification = "periodicLogger is defined in correct scope")
  public MaintenanceResults cleanupWorkflows(MaintenanceConfiguration configuration) {
    if (configuration.archiveWorkflowsOlderThan != null || configuration.deleteArchivedWorkflowsOlderThan != null) {
      maintenanceDao.ensureValidArchiveTablesExist();
    }

    MaintenanceResults res = new MaintenanceResults();
    if (configuration.deleteArchivedWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(ARCHIVE, configuration, configuration.deleteArchivedWorkflowsOlderThan);
      res.deletedArchivedWorkflows = doAction("Deleting archived workflows", format("Deleting archived workflows older than %s, in batches of %s.", configuration.deleteWorkflowsOlderThan, configuration.batchSize),
              source, idList -> maintenanceDao.deleteWorkflows(ARCHIVE, idList));
    }
    if (configuration.archiveWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration, configuration.archiveWorkflowsOlderThan);
      res.archivedWorkflows = doAction("Archiving workflows", format("Archiving passive workflows older than %s, in batches of %s.", configuration.archiveWorkflowsOlderThan, configuration.batchSize),
              source, maintenanceDao::archiveWorkflows);
    }
    if (configuration.deleteWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration, configuration.deleteWorkflowsOlderThan);
      res.deletedWorkflows = doAction("Deleting workflows", format("Deleting passive workflows older than %s, in batches of %s.", configuration.deleteWorkflowsOlderThan, configuration.batchSize),
              source, idList -> maintenanceDao.deleteWorkflows(MAIN, idList));
    }
    if (configuration.deleteStatesOlderThan != null) {
      // TODO
    }
    return res;
  }

  private Supplier<List<Long>> getIdQuery(TablePrefix table, MaintenanceConfiguration configuration, ReadablePeriod period) {
    DateTime olderThan = now().minus(period);
    return () -> maintenanceDao.listOldWorkflows(table, olderThan, configuration.batchSize);
  }

  private int doAction(String type, String description, Supplier<List<Long>> getActionables, Function<List<Long>, Integer> doAction) {
    log.info(description);
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
    public final ReadablePeriod deleteArchivedWorkflowsOlderThan;
    public final ReadablePeriod archiveWorkflowsOlderThan;
    public final ReadablePeriod deleteWorkflowsOlderThan;
    public final ReadablePeriod deleteStatesOlderThan;
    public final int batchSize;

    MaintenanceConfiguration(ReadablePeriod deleteArchivedWorkflowsOlderThan, ReadablePeriod archiveWorkflowsOlderThan,
        ReadablePeriod deleteWorkflowsOlderThan, ReadablePeriod deleteStatesOlderThan, int batchSize) {
      this.deleteArchivedWorkflowsOlderThan = deleteArchivedWorkflowsOlderThan;
      this.archiveWorkflowsOlderThan = archiveWorkflowsOlderThan;
      this.deleteWorkflowsOlderThan = deleteWorkflowsOlderThan;
      this.deleteStatesOlderThan = deleteStatesOlderThan;
      this.batchSize = batchSize;
    }

    public static class Builder {
      private ReadablePeriod deleteArchivedWorkflowsOlderThan;
      private ReadablePeriod archiveWorkflowsOlderThan;
      private ReadablePeriod deleteWorkflowsOlderThan;
      private ReadablePeriod deleteStatesOlderThan;
      private Integer batchSize = 1000;

      public Builder setDeleteArchivedWorkflowsOlderThan(ReadablePeriod deleteArchivedWorkflowsOlderThan) {
        this.deleteArchivedWorkflowsOlderThan = deleteArchivedWorkflowsOlderThan;
        return this;
      }

      public Builder setArchiveWorkflowsOlderThan(ReadablePeriod archiveWorkflowsOlderThan) {
        this.archiveWorkflowsOlderThan = archiveWorkflowsOlderThan;
        return this;
      }

      public Builder setDeleteWorkflowsOlderThan(ReadablePeriod deleteWorkflowsOlderThan) {
        this.deleteWorkflowsOlderThan = deleteWorkflowsOlderThan;
        return this;
      }

      public Builder setDeleteStatesOlderThan(ReadablePeriod deleteStatesOlderThan) {
        this.deleteStatesOlderThan = deleteStatesOlderThan;
        return this;
      }

      /**
       * @param batchSize
       *          Number of workflows to operate on in single transaction. Typical value is 100-1000. This parameter mostly
       *          affects on archiving performance.
       */
      public Builder setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
      }

      public MaintenanceConfiguration build() {
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        return new MaintenanceConfiguration(deleteArchivedWorkflowsOlderThan, archiveWorkflowsOlderThan, deleteWorkflowsOlderThan, deleteStatesOlderThan, batchSize);
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
