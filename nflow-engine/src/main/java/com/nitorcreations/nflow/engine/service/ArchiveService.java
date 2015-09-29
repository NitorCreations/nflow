package com.nitorcreations.nflow.engine.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.nitorcreations.nflow.engine.internal.util.PeriodicLogger;
import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.dao.ArchiveDao;

@Named
public class ArchiveService {
  private static final Logger log = getLogger(ArchiveService.class);
  @Inject
  private ArchiveDao archiveDao;

  /**
   * Archive old (whose modified time is earlier than <code>olderThan</code> parameter) and passive (that do not have
   * <code>nextActivation</code>) workflows. Copies workflow instances, workflow instance actions and state variables to
   * corresponding archive tables and removes them from production tables.
   *
   * @param olderThan Passive workflow instances whose modified time is before this will be archived.
   * @param batchSize Number of workflow hierarchies to archive in a single transaction. Typical value is 1-20. This parameter 
   * mostly affects on archiving performance.
   * @return Total number of archived workflows.
   */
  public int archiveWorkflows(DateTime olderThan, int batchSize) {
    Assert.notNull(olderThan, "olderThan must not be null");
    Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
    archiveDao.ensureValidArchiveTablesExist();
    log.info("Archiving starting. Archiving passive workflows older than {}, in batches of {}.", olderThan, batchSize);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    List<Integer> workflowIds;
    PeriodicLogger periodicLogger = new PeriodicLogger(log, 60);
    int archivedWorkflows = 0;
    do {
      workflowIds = archiveDao.listArchivableWorkflows(olderThan, batchSize);
      if (workflowIds.isEmpty()) {
        break;
      }
      archiveDao.archiveWorkflows(workflowIds);
      archivedWorkflows += workflowIds.size();

      double timeDiff = stopWatch.getTime() / 1000.0;
      log.debug("Archived {} workflows. {} workflows / second. Workflow ids: {}. ", workflowIds.size(), archivedWorkflows
          / timeDiff, workflowIds);
      periodicLogger.log("Archived {} workflows. Archiving about {} workflows / second.", workflowIds.size(), archivedWorkflows
              / timeDiff);
    } while (!workflowIds.isEmpty());

    log.info("Archiving finished. Archived {} workflows.", archivedWorkflows);
    return archivedWorkflows;
  }
}
