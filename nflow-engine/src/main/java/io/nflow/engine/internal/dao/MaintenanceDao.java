package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.ColumnNamesExtractor.columnNamesExtractor;
import static io.nflow.engine.internal.dao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.TablePrefix.MAIN;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;

@Named
public class MaintenanceDao {
  private static final Logger logger = getLogger(MaintenanceDao.class);

  private final SQLVariants sqlVariants;
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  private String workflowColumns;
  private String actionColumns;
  private String stateColumns;

  @Inject
  public MaintenanceDao(SQLVariants sqlVariants, @NFlow JdbcTemplate jdbcTemplate,
      @NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate) {
    this.sqlVariants = sqlVariants;
    this.jdbc = jdbcTemplate;
    this.namedJdbc = nflowNamedParameterJdbcTemplate;
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

  public List<Long> getOldWorkflowIds(TablePrefix table, DateTime before, int maxWorkflows, Set<String> workflowTypes) {
    StringBuilder sql = new StringBuilder("select id from ").append(table.nameOf("workflow"))
        .append(" where next_activation is null and ").append(sqlVariants.dateLtEqDiff("modified", "?"));
    List<Object> args = new ArrayList<>();
    args.add(sqlVariants.toTimestampObject(before));
    if (!workflowTypes.isEmpty()) {
      sql.append(" and type in (").append(workflowTypes.stream().map(t -> "?").collect(joining(","))).append(")");
      args.addAll(workflowTypes);
    }
    sql.append(" order by id asc");
    return jdbc.queryForList(sqlVariants.limit(sql.toString(), maxWorkflows), Long.class, args.toArray(new Object[0]));
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

  @Transactional
  public void deleteActionAndStateHistory(long workflowInstanceId, DateTime olderThan) {
    long start = currentTimeMillis();
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("workflowId", workflowInstanceId);
    params.addValue("olderThan", sqlVariants.toTimestampObject(olderThan));
    Long maxActionId = namedJdbc.queryForObject("select max(id) from nflow_workflow_action where workflow_id = :workflowId and "
        + sqlVariants.dateLtEqDiff("execution_end", ":olderThan"), params, Long.class);
    int deletedStates = 0;
    int deletedActions = 0;
    if (maxActionId != null) {
      params.addValue("maxActionId", maxActionId);
      List<Long> referredActionIds = namedJdbc.queryForList(
          "select distinct(max(action_id)) from nflow_workflow_state where workflow_id = :workflowId group by state_key", params,
          Long.class);
      String deleteStates = "delete from nflow_workflow_state where workflow_id = :workflowId and action_id <= :maxActionId";
      if (referredActionIds.isEmpty()) {
        deletedStates = namedJdbc.update(deleteStates, params);
      } else {
        params.addValue("referredActionIds", referredActionIds);
        deletedStates = namedJdbc.update(deleteStates + " and action_id not in (:referredActionIds)", params);
      }
      referredActionIds.addAll(namedJdbc.queryForList(
          "select distinct parent_action_id from nflow_workflow where parent_workflow_id = :workflowId", params, Long.class));
      String deleteActions = "delete from nflow_workflow_action where workflow_id = :workflowId and id <= :maxActionId";
      if (referredActionIds.isEmpty()) {
        deletedActions = namedJdbc.update(deleteActions, params);
      } else {
        params.addValue("referredActionIds", referredActionIds);
        deletedActions = namedJdbc.update(deleteActions + " and id not in (:referredActionIds)", params);
      }
    }
    if (deletedActions > 0 || deletedStates > 0) {
      logger.info("Deleted {} actions and {} states from workflow instance {} that were older than {}. Took {} ms.", deletedActions,
          deletedStates, workflowInstanceId, olderThan, currentTimeMillis() - start);
    }
  }
}
