package com.nitorcreations.nflow.engine.internal.dao;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.springframework.util.StringUtils.isEmpty;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@DependsOn("nflowDatabaseInitializer")
public class ExecutorDao {
  private final JdbcTemplate jdbc;
  final SQLVariants sqlVariants;

  private final int keepaliveIntervalSeconds;
  private DateTime nextUpdate = new DateTime(0);

  final String executorGroup;
  final String executorGroupCondition;
  final int timeoutSeconds;
  int executorId;


  @Inject
  public ExecutorDao(@Named("nflow-datasource") DataSource dataSource, Environment env, SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
    this.jdbc = new JdbcTemplate(dataSource);
    this.executorGroup = trimToNull(env.getProperty("nflow.executor.group"));
    this.executorGroupCondition = createWhereCondition(executorGroup);
    timeoutSeconds = env.getProperty("nflow.executor.timeout.seconds", Integer.class, (int) MINUTES.toSeconds(15));
    keepaliveIntervalSeconds = env.getProperty("nflow.executor.keepalive.seconds", Integer.class, (int) MINUTES.toSeconds(1));
  }

  private static String createWhereCondition(String group) {
    String groupCondition = "executor_group = '" + group + "'";
    if (isEmpty(group)) {
      groupCondition = "executor_group is null";
    }
    return groupCondition;
  }

  public void tick() {
    if (nextUpdate.isAfterNow()) {
      return;
    }
    nextUpdate = nextUpdate.plusSeconds(keepaliveIntervalSeconds);
    updateActiveTimestamp();
    recoverWorkflowInstancesFromDeadNodes();
  }

  public String getExecutorGroup() {
    return executorGroup;
  }

  public String getExecutorGroupCondition() {
    return executorGroupCondition;
  }

  public int getExecutorId() {
    return executorId;
  }

  @PostConstruct
  private void allocateExecutorId() {
    final String host;
    final int pid;
    try {
      host = InetAddress.getLocalHost().getCanonicalHostName();
      pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to obtain host name and pid of running jvm", ex);
    }
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement p = con.prepareStatement("insert into nflow_executor(host, pid, executor_group) values (?,?,?)", new String[] { "id" });
        p.setString(1, host);
        p.setInt(2, pid);
        p.setString(3, executorGroup);
        return p;
      }
    }, keyHolder);
    executorId = keyHolder.getKey().intValue();
  }

  public void updateActiveTimestamp() {
    jdbc.update("update nflow_executor set active=current_timestamp, expires=" + sqlVariants.currentTimePlusSeconds(timeoutSeconds) + " where id = " + executorId);
  }

  public void recoverWorkflowInstancesFromDeadNodes() {
    jdbc.update("update nflow_workflow set executor_id = null where executor_id in (select id from nflow_executor where " + executorGroupCondition + " and id <> " + executorId + " and expires < current_timestamp)");
  }
}
