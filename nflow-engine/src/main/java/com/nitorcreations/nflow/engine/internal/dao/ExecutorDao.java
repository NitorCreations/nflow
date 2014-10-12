package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.storage.db.DatabaseConfiguration.NFLOW_JDBC;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.joda.time.DateTime.now;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@DependsOn("nflowDatabaseInitializer")
public class ExecutorDao {
  private JdbcTemplate jdbc;
  final SQLVariants sqlVariants;
  private WorkflowInstanceDao workflowInstanceDao;

  private final int keepaliveIntervalSeconds;
  private DateTime nextUpdate = now();

  final String executorGroup;
  final String executorGroupCondition;
  final int timeoutSeconds;
  int executorId = -1;

  @Inject
  public ExecutorDao(Environment env, SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
    this.executorGroup = trimToNull(env.getRequiredProperty("nflow.executor.group"));
    this.executorGroupCondition = createWhereCondition(executorGroup);
    timeoutSeconds = env.getProperty("nflow.executor.timeout.seconds", Integer.class, (int) MINUTES.toSeconds(15));
    keepaliveIntervalSeconds = env.getProperty("nflow.executor.keepalive.seconds", Integer.class, (int) MINUTES.toSeconds(1));
  }

  /**
   * Use setter injection because constructor injection may not work
   * when nFlow is used in some legacy systems.
   * @param jdbcTemplate The JDBC template for accessing the nFlow data source.
   */
  @Inject
  public void setJdbcTemplate(@Named(NFLOW_JDBC) JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  @Inject
  public void setWorkflowInstanceDao(WorkflowInstanceDao workflowInstanceDao) {
    this.workflowInstanceDao = workflowInstanceDao;
  }

  private static String createWhereCondition(String group) {
    return "executor_group = '" + group + "'";
  }

  public void tick() {
    if (nextUpdate.isAfterNow()) {
      return;
    }
    nextUpdate = now().plusSeconds(keepaliveIntervalSeconds);
    updateActiveTimestamp();
    recoverWorkflowInstancesFromDeadNodes();
  }

  public String getExecutorGroup() {
    return executorGroup;
  }

  public String getExecutorGroupCondition() {
    return executorGroupCondition;
  }

  public synchronized int getExecutorId() {
    if (executorId == -1) {
      executorId = allocateExecutorId();
    }
    return executorId;
  }

  public DateTime getMaxWaitUntil() {
    return nextUpdate;
  }

  private int allocateExecutorId() {
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
    return keyHolder.getKey().intValue();
  }

  public void updateActiveTimestamp() {
    updateWithPreparedStatement("update nflow_executor set active=current_timestamp, expires=" + sqlVariants.currentTimePlusSeconds(timeoutSeconds) + " where id = " + getExecutorId());
  }

  public void recoverWorkflowInstancesFromDeadNodes() {
    List<InstanceInfo> instances = jdbc.query(
        "select id, state from nflow_workflow where executor_id in (select id from nflow_executor where "
            + executorGroupCondition + " and id <> ? and expires < current_timestamp)", new RowMapper<InstanceInfo>() {
          @Override
          public InstanceInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            InstanceInfo instance = new InstanceInfo();
            instance.id = rs.getInt(1);
            instance.state = rs.getString(2);
            return instance;
          }
        }, getExecutorId());
    for (InstanceInfo instance : instances) {
      int updated = jdbc.update("update nflow_workflow set executor_id = null where id = ? and executor_id in (select id from nflow_executor where " + executorGroupCondition + " and id <> ? and expires < current_timestamp)",
          instance.id, getExecutorId());
      if (updated > 0) {
        WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setExecutionStart(now()).setExecutionEnd(now())
            .setExecutorId(getExecutorId()).setState(instance.state).setStateText("Recovered").setWorkflowInstanceId(instance.id).build();
        workflowInstanceDao.insertWorkflowInstanceAction(action);
      }
    }
  }

  static final class InstanceInfo {
    public int id;
    public String state;
  }

  private void updateWithPreparedStatement(String sql) {
    // jdbc.update(sql) won't use prepared statements, this uses.
    jdbc.update(sql, (PreparedStatementSetter)null);
  }
}
