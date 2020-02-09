package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.ColumnNamesExtractor.columnNamesExtractor;
import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.MAIN;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;

@Named
public class MaintenanceDao {
  private static final Logger logger = getLogger(MaintenanceDao.class);

  private final JdbcTemplate jdbc;
  private final TableMetadataChecker tableMetadataChecker;
  private final SQLVariants sqlVariants;

  private String workflowColumns;
  private String actionColumns;
  private String stateColumns;

  @Inject
  public MaintenanceDao(SQLVariants sqlVariants, @NFlow JdbcTemplate jdbcTemplate, TableMetadataChecker tableMetadataChecker) {
    this.sqlVariants = sqlVariants;
    this.jdbc = jdbcTemplate;
    this.tableMetadataChecker = tableMetadataChecker;
  }

  @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "tableMetadataChecker is injected")
  public void ensureValidArchiveTablesExist() {
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow", "nflow_archive_workflow");
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow_action", "nflow_archive_workflow_action");
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow_state", "nflow_archive_workflow_state");
  }

  private String getWorkflowColumns() {
    if (isBlank(workflowColumns)) {
      workflowColumns = columnsFromMetadata("nflow_workflow");
    }
    return workflowColumns;
  }

  private String getActionColumns() {
    if (isBlank(actionColumns)) {
      actionColumns = columnsFromMetadata("nflow_workflow_action");
    }
    return actionColumns;
  }

  private String getStateColumns() {
    if (isBlank(stateColumns)) {
      stateColumns = columnsFromMetadata("nflow_workflow_state");
    }
    return stateColumns;
  }

  public List<Long> listOldWorkflows(TablePrefix table, DateTime before, int maxWorkflows) {
    String sql = sqlVariants.limit("select id from " + table.nameOf("workflow") + " where next_activation is null and "
        + sqlVariants.dateLtEqDiff("modified", "?") + " order by id asc", maxWorkflows);
    return jdbc.queryForList(sql, Long.class, sqlVariants.toTimestampObject(before));
  }

  @Transactional
  public int archiveWorkflows(Collection<Long> workflowIds) {
    String workflowIdParams = params(workflowIds);
    int archivedInstances = archiveTable("workflow", "id", getWorkflowColumns(), workflowIdParams);
    int archivedActions = archiveTable("workflow_action", "workflow_id", getActionColumns(), workflowIdParams);
    int archivedStates = archiveTable("workflow_state", "workflow_id", getStateColumns(), workflowIdParams);
    logger.info("Archived {} workflow instances, {} actions and {} states.", archivedInstances, archivedActions, archivedStates);
    deleteWorkflows(MAIN, workflowIdParams);
    return archivedInstances;
  }

  @Transactional
  public int deleteWorkflows(TablePrefix table, Collection<Long> workflowIds) {
    String workflowIdParams = params(workflowIds);
    return deleteWorkflows(table, workflowIdParams);
  }

  private int archiveTable(String table, String workflowIdColumn, String columns, String workflowIdParams) {
    return jdbc.update("insert into " + ARCHIVE.nameOf(table) + "(" + columns + ") " + "select " + columns + " from "
        + MAIN.nameOf(table) + " where " + workflowIdColumn + " in " + workflowIdParams + sqlVariants.forUpdateInnerSelect());
  }

  private int deleteWorkflows(TablePrefix table, String workflowIdParams) {
    int deletedStates = jdbc.update("delete from " + table.nameOf("workflow_state") + " where workflow_id in " + workflowIdParams);
    int deletedActions = jdbc.update("delete from " + table.nameOf("workflow_action") + " where workflow_id in " + workflowIdParams);
    int deletedInstances = jdbc.update("delete from " + table.nameOf("workflow") + " where id in " + workflowIdParams);
    logger.info("Deleted {} workflow instances, {} actions and {} states from {} tables.", deletedInstances, deletedActions,
        deletedStates, table.name());
    return deletedInstances;
  }

  private String columnsFromMetadata(String tableName) {
    List<String> columnNames = jdbc.query("select * from " + tableName + " where 1 = 0", columnNamesExtractor);
    return join(columnNames, ",");
  }

  private String params(Collection<Long> workflowIds) {
    return "(" + join(workflowIds, ",") + ")";
  }

  public enum TablePrefix {
    MAIN("nflow_"),
    ARCHIVE("nflow_archive_");

    private final String prefix;

    TablePrefix(String prefix) {
      this.prefix = prefix;
    }

    String nameOf(String name) {
      return prefix + name;
    }
  }
}
