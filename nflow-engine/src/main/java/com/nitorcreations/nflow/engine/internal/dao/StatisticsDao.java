package com.nitorcreations.nflow.engine.internal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics.QueueStatistics;

@Component
@DependsOn("nflowDatabaseInitializer")
public class StatisticsDao {
  private JdbcTemplate jdbc;
  private ExecutorDao executorInfo;

  @Inject
  public void setExecutorDao(ExecutorDao executorDao) {
    this.executorInfo = executorDao;
  }

  /**
   * Use setter injection because having the dataSource in constructor may not work
   * when nFlow is used in some legacy systems.
   * @param dataSource The nFlow data source.
   */
  @Inject
  public void setDataSource(@Named("nflowDatasource") DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
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
      if(itemsOnly) {
        return new QueueStatistics(items, null, null);
      }
      Timestamp oldest = rs.getTimestamp("oldest");
      Timestamp newest = rs.getTimestamp("newest");

      Timestamp now = rs.getTimestamp("dbtime");
      return new QueueStatistics(items, toMillis(oldest, now), toMillis(newest, now));
    }

    private long toMillis(Timestamp ts, Timestamp now) {
      if(ts == null) {
        return 0;
      }
      return new DateTime(now).getMillis() - new DateTime(ts).getMillis();
    }
  }
}
