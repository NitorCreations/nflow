package com.nitorcreations.nflow.engine.internal.dao;

import static java.util.Arrays.asList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;

/**
 * Use setter injection because constructor injection may not work when nFlow is
 * used in some legacy systems.
 */
@Component
public class StatisticsDao {

  private JdbcTemplate jdbc;
  private ExecutorDao executorInfo;

  @Inject
  public void setExecutorDao(ExecutorDao executorDao) {
    this.executorInfo = executorDao;
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  public Statistics getQueueStatistics() {
    return new Statistics(queuedStatistics(), executionStatistics());
  }

  private QueueStatistics executionStatistics() {
    String sql = "select count(1) items, current_timestamp oldest, current_timestamp newest, current_timestamp dbtime "
        + "from nflow_workflow "
        + "where executor_id is not null and "
        + executorInfo.getExecutorGroupCondition();
    return jdbc.query(sql, new StatisticsExtractor(true));
  }

  private QueueStatistics queuedStatistics() {
    String sql = "select count(1) items, min(next_activation) oldest, max(next_activation) newest, current_timestamp dbtime "
        + "from nflow_workflow "
        + "where next_activation < current_timestamp and executor_id is null and "
        + executorInfo.getExecutorGroupCondition();
    return jdbc.query(sql, new StatisticsExtractor(false));
  }

  static class StatisticsExtractor implements ResultSetExtractor<QueueStatistics> {
    private final boolean itemsOnly;
    public StatisticsExtractor(boolean itemsOnly) {
      this.itemsOnly = itemsOnly;
    }
    @Override
    public QueueStatistics extractData(ResultSet rs) throws SQLException, DataAccessException {
      rs.next();
      int items = rs.getInt("items");
      if (itemsOnly) {
        return new QueueStatistics(items, null, null);
      }
      Timestamp oldest = rs.getTimestamp("oldest");
      Timestamp newest = rs.getTimestamp("newest");

      Timestamp now = rs.getTimestamp("dbtime");
      return new QueueStatistics(items, toMillis(oldest, now), toMillis(newest, now));
    }

    private static long toMillis(Timestamp ts, Timestamp now) {
      if (ts == null) {
        return 0;
      }
      return now.getTime() - ts.getTime();
    }
  }

  public Map<String, Map<String, WorkflowDefinitionStatistics>> getWorkflowDefinitionStatistics(String type,
      DateTime createdAfter, DateTime createdBefore, DateTime modifiedAfter, DateTime modifiedBefore) {
    String executorGroup = executorInfo.getExecutorGroup();
    List<Object> argsList = new ArrayList<>();
    argsList.addAll(asList(executorGroup, type));
    StringBuilder whereBuilder = new StringBuilder();
    if (createdAfter != null) {
      whereBuilder.append(" and created >= ?");
      argsList.add(createdAfter.toDate());
    }
    if (createdBefore != null) {
      whereBuilder.append(" and created < ?");
      argsList.add(createdBefore.toDate());
    }
    if (modifiedAfter != null) {
      whereBuilder.append(" and modified >= ?");
      argsList.add(modifiedAfter.toDate());
    }
    if (modifiedBefore != null) {
      whereBuilder.append(" and modified < ?");
      argsList.add(modifiedBefore.toDate());
    }
    String where = whereBuilder.toString();
    StringBuilder sqlBuilder = new StringBuilder("select state, status, count(*) all_instances,")
        .append(" count(case when next_activation < current_timestamp then 1 else null end) queued_instances")
        .append(" from nflow_workflow where executor_group = ? and type = ?").append(where).append(" group by state, status");
    String query = sqlBuilder.toString();
    Object[] argsArray = argsList.toArray(new Object[argsList.size()]);
    final Map<String, Map<String, WorkflowDefinitionStatistics>> stats = new LinkedHashMap<>();
    jdbc.query(query, argsArray, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        Map<String, WorkflowDefinitionStatistics> stateStats = stats.get(state);
        if (stateStats == null) {
          stateStats = new LinkedHashMap<>();
          stats.put(state, stateStats);
        }
        String status = rs.getString("status");
        WorkflowDefinitionStatistics statusStats = new WorkflowDefinitionStatistics();
        statusStats.allInstances = rs.getLong("all_instances");
        statusStats.queuedInstances = rs.getLong("queued_instances");
        stateStats.put(status, statusStats);
      }
    });
    return stats;
  }

}
