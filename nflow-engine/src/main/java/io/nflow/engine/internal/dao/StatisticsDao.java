package io.nflow.engine.internal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import io.nflow.engine.workflow.statistics.Statistics;
import io.nflow.engine.workflow.statistics.Statistics.QueueStatistics;

/**
 * Use setter injection because constructor injection may not work when nFlow is
 * used in some legacy systems.
 */
@Component
@SuppressFBWarnings(value = { "SIC_INNER_SHOULD_BE_STATIC_ANON",
    "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" }, justification = "common jdbctemplate practice, jdbc and executorInfo are injected")
public class StatisticsDao {

  private final JdbcTemplate jdbc;
  private final ExecutorDao executorInfo;

  @Inject
  public StatisticsDao(@NFlow JdbcTemplate jdbcTemplate, ExecutorDao executorDao) {
    this.executorInfo = executorDao;
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
    public QueueStatistics extractData(ResultSet rs) throws SQLException {
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
    argsList.add(executorGroup);
    argsList.add(type);
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
    String query = "select state, status, count(*) all_instances," +
            " count(case when next_activation < current_timestamp then 1 else null end) queued_instances" +
            " from nflow_workflow where executor_group = ? and type = ?" + where + " group by state, status";
    Object[] argsArray = argsList.toArray(new Object[argsList.size()]);
    final Map<String, Map<String, WorkflowDefinitionStatistics>> stats = new LinkedHashMap<>();
    jdbc.query(query, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        Map<String, WorkflowDefinitionStatistics> stateStats = stats.computeIfAbsent(state, k -> new LinkedHashMap<>());
        String status = rs.getString("status");
        WorkflowDefinitionStatistics statusStats = new WorkflowDefinitionStatistics();
        statusStats.allInstances = rs.getLong("all_instances");
        statusStats.queuedInstances = rs.getLong("queued_instances");
        stateStats.put(status, statusStats);
      }
    }, argsArray);
    return stats;
  }

}
