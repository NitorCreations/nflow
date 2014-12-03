package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static com.nitorcreations.nflow.engine.internal.storage.db.DatabaseConfiguration.NFLOW_DATABASE_INITIALIZER;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.joda.time.DateTime.now;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

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
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Use setter injection because constructor injection may not work when nFlow is
 * used in some legacy systems.
 */
@Component
@DependsOn(NFLOW_DATABASE_INITIALIZER)
public class ExecutorDao {
  private JdbcTemplate jdbc;
  SQLVariants sqlVariants;
  private WorkflowInstanceDao workflowInstanceDao;

  private int keepaliveIntervalSeconds;
  private DateTime nextUpdate = now();

  String executorGroup;
  String executorGroupCondition;
  int timeoutSeconds;
  int executorId = -1;

  @Inject
  public void setEnvironment(Environment env) {
    this.executorGroup = trimToNull(env.getRequiredProperty("nflow.executor.group"));
    this.executorGroupCondition = createWhereCondition(executorGroup);
    timeoutSeconds = env.getProperty("nflow.executor.timeout.seconds", Integer.class, (int) MINUTES.toSeconds(15));
    keepaliveIntervalSeconds = env.getProperty("nflow.executor.keepalive.seconds", Integer.class, (int) MINUTES.toSeconds(1));
  }

  @Inject
  public void setSQLVariants(SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate nflowJdbcTemplate) {
    this.jdbc = nflowJdbcTemplate;
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

  @Transactional
  public boolean isTransactionSupportEnabled() {
    return isActualTransactionActive();
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

  public List<WorkflowExecutor> getExecutors() {
    return jdbc.query("select * from nflow_executor where executor_group = ? order by id asc", new RowMapper<WorkflowExecutor>() {
      @Override
      public WorkflowExecutor mapRow(ResultSet rs, int rowNum) throws SQLException {
        int id = rs.getInt("id");
        String host = rs.getString("host");
        int pid = rs.getInt("pid");
        DateTime started = toDateTime(rs.getTimestamp("started"));
        DateTime active = toDateTime(rs.getTimestamp("active"));
        DateTime expires = toDateTime(rs.getTimestamp("expires"));
        return new WorkflowExecutor(id, host, pid, executorGroup, started, active, expires);
      }
    }, executorGroup);
  }
}
