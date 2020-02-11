package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.TablePrefix.MAIN;
import static java.lang.Math.max;
import static java.lang.String.format;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.TablePrefix;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.MaintenanceResults.Builder;

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

    Builder builder = new MaintenanceResults.Builder();
    if (configuration.deleteArchivedWorkflows != null) {
      builder.setDeletedArchivedWorkflows(doAction("Deleting archived workflows", configuration.deleteArchivedWorkflows, ARCHIVE,
          idList -> maintenanceDao.deleteWorkflows(ARCHIVE, idList)));
    }
    if (configuration.archiveWorkflows != null) {
      builder.setArchivedWorkflows(
          doAction("Archiving workflows", configuration.archiveWorkflows, MAIN, maintenanceDao::archiveWorkflows));
    }
    if (configuration.deleteWorkflows != null) {
      builder.setDeletedWorkflows(doAction("Deleting workflows", configuration.deleteWorkflows, MAIN,
          idList -> maintenanceDao.deleteWorkflows(MAIN, idList)));
    }
    if (configuration.deleteStates != null) {
      // TODO
    }
    return builder.build();
  }

  private int doAction(String type, ConfigurationItem configuration, TablePrefix table, Function<List<Long>, Integer> doAction) {
    DateTime olderThan = now().minus(configuration.olderThanPeriod);
    log.info("{} older than {}, in batches of {}.", type, olderThan, configuration.batchSize);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    PeriodicLogger periodicLogger = new PeriodicLogger(log, 60);
    int totalWorkflows = 0;
    do {
      List<Long> workflowIds = maintenanceDao.listOldWorkflows(table, olderThan, configuration.batchSize);
      if (workflowIds.isEmpty()) {
        break;
      }
      int workflows = doAction.apply(workflowIds);
      totalWorkflows += workflows;
      double timeDiff = max(stopWatch.getTime() / 1000.0, 0.000001);
      String status = format("%s. %s workflows, %.1f workflows / second.", type, workflows, totalWorkflows / timeDiff);
      log.debug("{} Workflow ids: {}.", status, workflowIds);
      periodicLogger.info(status);
    } while (true);
    log.info("{} finished. Operated on {} workflows in {} seconds.", type, totalWorkflows, stopWatch.getTime() / 1000);
    return totalWorkflows;
  }
}
