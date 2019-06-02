package io.nflow.engine.internal.dao;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static io.nflow.engine.internal.dao.DaoUtil.ColumnNamesExtractor.columnNamesExtractor;
import static org.apache.commons.lang3.StringUtils.join;

@Named
public class ArchiveDao {
  private JdbcTemplate jdbc;
  private TableMetadataChecker tableMetadataChecker;
  private SQLVariants sqlVariants;

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

  public List<Integer> listArchivableWorkflows(DateTime before, int maxRows) {
    return jdbc.query(
                    "select w.id id from nflow_workflow w, " +
                    "(" + sqlVariants.limit(
                    "  select parent.id from nflow_workflow parent " +
                    "  where parent.next_activation is null and " + sqlVariants.dateLtEqDiff("parent.modified", "?") +
                    "  and parent.root_workflow_id is null " +
                    "  and not exists(" +
                    "    select 1 from nflow_workflow child where child.root_workflow_id = parent.id " +
                    "      and (" + sqlVariants.dateLtEqDiff("?", "child.modified") + " or child.next_activation is not null)" +
                    "  )" +
                    "  order by modified asc ", maxRows) +
                    ") as archivable_parent " +
                    "where archivable_parent.id = w.id or archivable_parent.id = w.root_workflow_id",
            new ArchivableWorkflowsRowMapper(), sqlVariants.toTimestampObject(before), sqlVariants.toTimestampObject(before));
  }

  @Transactional
  public int archiveWorkflows(List<Integer> workflowIds) {
    String workflowIdParams = params(workflowIds);

    int archivedWorkflows = archiveWorkflowTable(workflowIdParams);
    archiveActionTable(workflowIdParams);
    archiveStateTable(workflowIdParams);
    deleteWorkflows(workflowIdParams);
    return archivedWorkflows;
  }

  private int archiveWorkflowTable(String workflowIdParams) {
    String columns = columnsFromMetadata("nflow_workflow");
    return jdbc.update("insert into nflow_archive_workflow(" + columns + ") " +
            "select " + columns + " from nflow_workflow where id in " + workflowIdParams);
  }

  private void archiveActionTable(String workflowIdParams) {
    String columns = columnsFromMetadata("nflow_workflow_action");
    jdbc.update("insert into nflow_archive_workflow_action(" + columns + ") " +
            "select " + columns + " from nflow_workflow_action where workflow_id in " + workflowIdParams);
  }

  private void archiveStateTable(String workflowIdParams) {
    String columns = columnsFromMetadata("nflow_workflow_state");
    jdbc.update("insert into nflow_archive_workflow_state (" + columns + ") " +
            "select " + columns + " from nflow_workflow_state where workflow_id in " + workflowIdParams);
  }

  private void deleteWorkflows(String workflowIdParams) {
    jdbc.update("delete from nflow_workflow_state where workflow_id in " + workflowIdParams);
    jdbc.update("update nflow_workflow set root_workflow_id=null, parent_workflow_id=null, parent_action_id=null " +
            "where id in " + workflowIdParams + " and (root_workflow_id is not null or parent_workflow_id is not null)");
    jdbc.update("delete from nflow_workflow_action where workflow_id in " + workflowIdParams);
    jdbc.update("delete from nflow_workflow where id in " + workflowIdParams);
  }

  private String columnsFromMetadata(String tableName) {
    List<String> columnNames = jdbc.query("select * from " + tableName + " where 1 = 0", columnNamesExtractor);
    return join(columnNames, ",");
  }

  private String params(List<Integer> workflowIds) {
    return "(" + join(workflowIds, ",") + ")";
  }

  static class ArchivableWorkflowsRowMapper implements RowMapper<Integer> {
    @Override
    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
      return rs.getInt("id");
    }
  }
}
