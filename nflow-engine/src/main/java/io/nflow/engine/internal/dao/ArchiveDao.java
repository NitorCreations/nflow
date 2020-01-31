package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.ArchiveDao.TablePrefix.MAIN;
import static io.nflow.engine.internal.dao.DaoUtil.ColumnNamesExtractor.columnNamesExtractor;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;

@Named
public class ArchiveDao {
  private static final Logger logger = getLogger(ArchiveDao.class);

  private final JdbcTemplate jdbc;
  private final TableMetadataChecker tableMetadataChecker;
  private final SQLVariants sqlVariants;

  private String workflowColumns;
  private String actionColumns;
  private String stateColumns;

  @Inject
  public ArchiveDao(SQLVariants sqlVariants, @NFlow JdbcTemplate jdbcTemplate, TableMetadataChecker tableMetadataChecker) {
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
    return jdbc.query(
                    sqlVariants.limit(
                    "select id from " + table.nameOf("workflow") +
                    "  where next_activation is null and " + sqlVariants.dateLtEqDiff("modified", "?") +
                    "  order by modified asc ", maxWorkflows),
            new ArchivableWorkflowsRowMapper(), sqlVariants.toTimestampObject(before));
  }

  public List<Long> listOldWorkflowTrees(TablePrefix table, DateTime before, int maxTrees) {
    return jdbc.query(
            "select w.id id from nflow_workflow w, " +
                    "(" + sqlVariants.limit(
                    "  select parent.id from " + table.nameOf("workflow") + " parent " +
                            "  where parent.next_activation is null and " + sqlVariants.dateLtEqDiff("parent.modified", "?") +
                            "  and parent.root_workflow_id is null " +
                            "  and not exists(" +
                            "    select 1 from " + table.nameOf("workflow") + " child where child.root_workflow_id = parent.id " +
                            "      and (" + sqlVariants.dateLtEqDiff("?", "child.modified") + " or child.next_activation is not null)" +
                            "  )" +
                            "  order by modified asc ", maxTrees) +
                    ") as archivable_parent " +
                    "where archivable_parent.id = w.id or archivable_parent.id = w.root_workflow_id",
            new ArchivableWorkflowsRowMapper(), sqlVariants.toTimestampObject(before), sqlVariants.toTimestampObject(before));
  }

  @Transactional
  public int archiveWorkflows(Collection<Long> workflowIds) {
    String workflowIdParams = params(workflowIds);
    int archivedInstances = archiveWorkflowTable(workflowIdParams);
    int archivedActions = archiveActionTable(workflowIdParams);
    int archivedStates = archiveStateTable(workflowIdParams);
    logger.info("Archived {} workflow instances, {} actions and {} states.", archivedInstances, archivedActions, archivedStates);
    deleteWorkflows(MAIN, workflowIdParams);
    return archivedInstances;
  }

  @Transactional
  public int deleteWorkflows(TablePrefix table, Collection<Long> workflowIds) {
    String workflowIdParams = params(workflowIds);
    int deletedWorkflows = deleteWorkflows(table, workflowIdParams);
    return deletedWorkflows;
  }

  private int archiveWorkflowTable(String workflowIdParams) {
    return jdbc.update("insert into nflow_archive_workflow(" + getWorkflowColumns() + ") " + "select " + getWorkflowColumns()
        + " from nflow_workflow where id in " + workflowIdParams + sqlVariants.forUpdateInnerSelect());
  }

  private int archiveActionTable(String workflowIdParams) {
    return jdbc.update("insert into nflow_archive_workflow_action(" + getActionColumns() + ") " + "select " + getActionColumns()
        + " from nflow_workflow_action where workflow_id in " + workflowIdParams + sqlVariants.forUpdateInnerSelect());
  }

  private int archiveStateTable(String workflowIdParams) {
    return jdbc.update("insert into nflow_archive_workflow_state (" + getStateColumns() + ") " + "select " + getStateColumns()
        + " from nflow_workflow_state where workflow_id in " + workflowIdParams + sqlVariants.forUpdateInnerSelect());
  }

  private int deleteWorkflows(TablePrefix table, String workflowIdParams) {
    int deletedStates = jdbc.update("delete from " + table.nameOf("workflow_state") + " where workflow_id in " + workflowIdParams);
    int deletedActions=jdbc.update("delete from " + table.nameOf("workflow_action") + " where workflow_id in " + workflowIdParams);
    int deletedInstances= jdbc.update("delete from " + table.nameOf("workflow") + " where id in " + workflowIdParams);
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

  static class ArchivableWorkflowsRowMapper implements RowMapper<Long> {
    @Override
    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
      return rs.getLong("id");
    }
  }
}
