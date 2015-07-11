package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.storage.db.DatabaseConfiguration.NFLOW_DATABASE_INITIALIZER;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.internal.config.NFlow;

@Named
@DependsOn(NFLOW_DATABASE_INITIALIZER)
public class ArchiveDao {
  private JdbcTemplate jdbc;
  private TableMetadataChecker tableMetadataChecker;

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  @Inject
  public void setTableMetadataChecker(TableMetadataChecker tableMetadataChecker) {
    this.tableMetadataChecker = tableMetadataChecker;
  }

  public void ensureValidArchiveTablesExist() {
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow", "nflow_archive_workflow");
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow_action", "nflow_archive_workflow_action");
    tableMetadataChecker.ensureCopyingPossible("nflow_workflow_state", "nflow_archive_workflow_state");
  }

  public List<Integer> listArchivableWorkflows(DateTime before, int maxRows) {
    return jdbc.query("select * from nflow_workflow parent where parent.next_activation is null and parent.modified <= ? " +
                    "and not exists(" +
                    "  select 1 from nflow_workflow child where child.root_workflow_id = parent.id " +
                    "    and (child.modified > ? or child.next_activation is not null)" +
                    ")" +
                    "order by modified asc " +
                    "limit " + maxRows,
            new Object[]{DaoUtil.toTimestamp(before), DaoUtil.toTimestamp(before)}, new ArchivableWorkflowsRowMapper());

    // TODO add index to nflow_workflow.modified (combined index with next_activation?)
    // TODO change modified trigger for postgre
    // TODO add new triggers for h2 and postgre to update scripts
    // TODO implement method to check that archive and prod tables have matching fields
  }

  @Transactional
  public void archiveWorkflows(List<Integer> workflowIds) {
    String workflowIdParams = params(workflowIds);

    archiveWorkflowTable(workflowIdParams);
    archiveActionTable(workflowIdParams);
    archiveStateTable(workflowIdParams);
    deleteWorkflows(workflowIdParams);
  }

  private void archiveWorkflowTable(String workflowIdParams) {
    String columns = columnsFromMetadata("nflow_workflow");
    jdbc.update("insert into nflow_archive_workflow(" + columns + ") " +
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
            "where id in " + workflowIdParams);
    jdbc.update("delete from nflow_workflow_action where workflow_id in " + workflowIdParams);
    jdbc.update("delete from nflow_workflow where id in " + workflowIdParams);
  }

  private String columnsFromMetadata(String tableName) {
    List<String> columnNames = jdbc.query("select * from " + tableName + " where 1 = 0", DaoUtil.ColumnNamesExtractor.columnNamesExtractor);
    return StringUtils.join(columnNames.toArray(), ",");
  }

  private String params(List<Integer> workflowIds) {
    return "(" + StringUtils.join(workflowIds.toArray(), ",") + ")";
  }

  private static class ArchivableWorkflowsRowMapper implements RowMapper<Integer> {
    @Override
    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
      return rs.getInt("id");
    }
  }
}
