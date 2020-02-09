package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.ArchiveDao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.ArchiveDao.TablePrefix.MAIN;
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
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.ArchiveDao;
import io.nflow.engine.internal.dao.ArchiveDao.TablePrefix;
import io.nflow.engine.internal.util.PeriodicLogger;

/**
 * Service for archiving old workflow instances from nflow-tables to nflow_archive-tables.
 */
@Named
public class ArchiveService {
  private static final Logger log = getLogger(ArchiveService.class);

  private final ArchiveDao archiveDao;

  @Inject
  public ArchiveService(ArchiveDao archiveDao) {
    this.archiveDao = archiveDao;
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
  public ArchiveResults cleanupWorkflows(ArchiveConfiguration configuration) {
    if (configuration.archiveWorkflowsOlderThan != null || configuration.deleteArchivedWorkflowsOlderThan != null) {
      archiveDao.ensureValidArchiveTablesExist();
    }

    ArchiveResults res = new ArchiveResults();
    if (configuration.deleteArchivedWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(ARCHIVE, configuration, configuration.deleteArchivedWorkflowsOlderThan);
      res.deletedWorkflows = doAction("Deleting archived workflows", format("Deleting archived workflows older than %s, in batches of %s.", configuration.deleteWorkflowsOlderThan, configuration.batchSize),
              source, idList -> archiveDao.deleteWorkflows(ARCHIVE, idList));
    }
    if (configuration.archiveWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration, configuration.archiveWorkflowsOlderThan);
      res.archivedWorkflows = doAction("Archiving workflows", format("Archiving passive workflows older than %s, in batches of %s.", configuration.archiveWorkflowsOlderThan, configuration.batchSize),
              source, archiveDao::archiveWorkflows);
    }
    if (configuration.deleteWorkflowsOlderThan != null) {
      Supplier<List<Long>> source = getIdQuery(MAIN, configuration, configuration.deleteWorkflowsOlderThan);
      res.deletedWorkflows = doAction("Deleting workflows", format("Deleting passive workflows older than %s, in batches of %s.", configuration.deleteWorkflowsOlderThan, configuration.batchSize),
              source, idList -> archiveDao.deleteWorkflows(MAIN, idList));
    }
    if (configuration.deleteStatesOlderThan != null) {
      // TODO
    }
    return res;
  }

  private Supplier<List<Long>> getIdQuery(TablePrefix table, ArchiveConfiguration configuration, Duration duration) {
    DateTime olderThan = now().minus(duration);
    return () -> archiveDao.listOldWorkflows(table, olderThan, configuration.batchSize);
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

  public static class ArchiveConfiguration {
    public final Duration deleteArchivedWorkflowsOlderThan;
    public final Duration archiveWorkflowsOlderThan;
    public final Duration deleteWorkflowsOlderThan;
    public final Duration deleteStatesOlderThan;
    public final int batchSize;

    ArchiveConfiguration(Duration deleteArchivedWorkflowsOlderThan, Duration archiveWorkflowsOlderThan,
        Duration deleteWorkflowsOlderThan, Duration deleteStatesOlderThan, int batchSize) {
      this.deleteArchivedWorkflowsOlderThan = deleteArchivedWorkflowsOlderThan;
      this.archiveWorkflowsOlderThan = archiveWorkflowsOlderThan;
      this.deleteWorkflowsOlderThan = deleteWorkflowsOlderThan;
      this.deleteStatesOlderThan = deleteStatesOlderThan;
      this.batchSize = batchSize;
    }

    public static class Builder {
      private Duration deleteArchivedWorkflowsOlderThan;
      private Duration archiveWorkflowsOlderThan;
      private Duration deleteWorkflowsOlderThan;
      private Duration deleteStatesOlderThan;
      private Integer batchSize;

      public Builder setDeleteArchivedWorkflowsOlderThan(Duration deleteArchivedWorkflowsOlderThan) {
        this.deleteArchivedWorkflowsOlderThan = deleteArchivedWorkflowsOlderThan;
        return this;
      }

      public Builder setArchiveWorkflowsOlderThan(Duration archiveWorkflowsOlderThan) {
        this.archiveWorkflowsOlderThan = archiveWorkflowsOlderThan;
        return this;
      }

      public Builder setDeleteWorkflowsOlderThan(Duration deleteWorkflowsOlderThan) {
        this.deleteWorkflowsOlderThan = deleteWorkflowsOlderThan;
        return this;
      }

      public Builder setDeleteStatesOlderThan(Duration deleteStatesOlderThan) {
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

      public ArchiveConfiguration build() {
        if (batchSize == null) {
          batchSize = 1000;
        } else {
          Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        }
        return new ArchiveConfiguration(deleteArchivedWorkflowsOlderThan, archiveWorkflowsOlderThan, deleteWorkflowsOlderThan,
            deleteStatesOlderThan, batchSize);
      }
    }
  }

  public static class ArchiveResults {
    public int deletedArchivedWorkflows;
    public int archivedWorkflows;
    public int deletedWorkflows;
    public int deletedStates;
  }
}
