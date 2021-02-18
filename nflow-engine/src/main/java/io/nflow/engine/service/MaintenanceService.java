package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.TablePrefix.MAIN;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.TableMetadataChecker;
import io.nflow.engine.internal.dao.TablePrefix;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.MaintenanceResults.Builder;

/**
 * Service for deleting and archiving old workflow instances from nflow-tables and nflow_archive-tables.
 */
@Named
public class MaintenanceService {

  private static final List<String> ARCHIVABLE_TABLES = asList("workflow", "workflow_action", "workflow_state");

  private static final Logger log = getLogger(MaintenanceService.class);

  private final MaintenanceDao maintenanceDao;

  private final TableMetadataChecker tableMetadataChecker;

  private final WorkflowDefinitionService workflowDefinitionService;

  @Inject
  public MaintenanceService(MaintenanceDao maintenanceDao, TableMetadataChecker tableMetadataChecker,
      WorkflowDefinitionService workflowDefinitionService) {
    this.maintenanceDao = maintenanceDao;
    this.tableMetadataChecker = tableMetadataChecker;
    this.workflowDefinitionService = workflowDefinitionService;
  }

  /**
   * Cleans up old (whose modified time is earlier than <code>olderThanPeriod</code> parameter) and passive (that do not have
   * <code>nextActivation</code>) workflows. Copies workflow instances, actions and state variables to corresponding archive
   * tables and removes them from production and archive tables as requested.
   *
   * @param configuration
   *          Cleanup actions to be executed and parameters for the actions.
   * @return Object describing the number of workflows acted on.
   */
  @SuppressFBWarnings(value = "BAS_BLOATED_ASSIGNMENT_SCOPE", justification = "periodicLogger is defined in correct scope")
  public MaintenanceResults cleanupWorkflows(MaintenanceConfiguration configuration) {
    validateConfiguration(configuration);
    if (configuration.archiveWorkflows != null || configuration.deleteArchivedWorkflows != null) {
      ARCHIVABLE_TABLES.forEach(table -> tableMetadataChecker.ensureCopyingPossible(MAIN.nameOf(table), ARCHIVE.nameOf(table)));
    }
    Builder builder = new MaintenanceResults.Builder();
    if (configuration.deleteArchivedWorkflows != null) {
      builder.setDeletedArchivedWorkflows(doAction("Deleting archived workflows", configuration.deleteArchivedWorkflows, ARCHIVE,
          idList -> maintenanceDao.deleteWorkflows(ARCHIVE, idList)));
    }
    if (configuration.archiveWorkflows != null) {
      builder.setArchivedWorkflows(
          doAction("Archiving workflows", configuration.archiveWorkflows, MAIN,
                  idList -> maintenanceDao.archiveWorkflows(idList, configuration.archiveWorkflows.archiveItemLimit)));
    }
    if (configuration.deleteWorkflows != null) {
      builder.setDeletedWorkflows(doAction("Deleting workflows", configuration.deleteWorkflows, MAIN,
          idList -> maintenanceDao.deleteWorkflows(MAIN, idList)));
    }
    return builder.build();
  }

  private void validateConfiguration(MaintenanceConfiguration configuration) {
    Stream.of(configuration.archiveWorkflows, configuration.deleteArchivedWorkflows, configuration.deleteWorkflows) //
        .filter(Objects::nonNull) //
        .flatMap(configItem -> configItem.workflowTypes.stream()) //
        .filter(workflowType -> workflowDefinitionService.getWorkflowDefinition(workflowType) == null) //
        .findAny() //
        .ifPresent(workflowType -> {
          throw new IllegalArgumentException("Workflow type " + workflowType + " is not valid");
        });
  }

  private int doAction(String type, ConfigurationItem configuration, TablePrefix table, Function<List<Long>, Integer> doAction) {
    DateTime olderThan = now().minus(configuration.olderThanPeriod);
    log.info("{} older than {}, in batches of {}.", type, olderThan, configuration.batchSize);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    PeriodicLogger periodicLogger = new PeriodicLogger(log, 60);
    int totalWorkflows = 0;
    do {
      List<Long> workflowIds = maintenanceDao.getOldWorkflowIds(table, olderThan, configuration.batchSize,
          configuration.workflowTypes);
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
