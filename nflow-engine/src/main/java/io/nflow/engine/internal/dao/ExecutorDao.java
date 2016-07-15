package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.firstColumnLengthExtractor;
import static io.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static java.net.InetAddress.getLocalHost;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.executor.WorkflowExecutor;

/**
 * Use setter injection because constructor injection may not work when nFlow is used in some legacy systems.
 */
@Component
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
public class ExecutorDao {
  private static final Logger logger = getLogger(ExecutorDao.class);
  private JdbcTemplate jdbc;
  SQLVariants sqlVariants;

  private int keepaliveIntervalSeconds;
  private DateTime nextUpdate = now();

  String executorGroup;
  String executorGroupCondition;
  int timeoutSeconds;
  int executorId = -1;
  int hostMaxLength;

  @Inject
  public void setEnvironment(Environment env) {
    this.executorGroup = trimToNull(env.getRequiredProperty("nflow.executor.group"));
    this.executorGroupCondition = createWhereCondition(executorGroup);
    timeoutSeconds = env.getRequiredProperty("nflow.executor.timeout.seconds", Integer.class);
    keepaliveIntervalSeconds = env.getRequiredProperty("nflow.executor.keepalive.seconds", Integer.class);
  }

  @Inject
  public void setSqlVariants(SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate nflowJdbcTemplate) {
    this.jdbc = nflowJdbcTemplate;
  }

  @PostConstruct
  public void findHostMaxLength() {
    hostMaxLength = jdbc.query("select host from nflow_executor where 1 = 0", firstColumnLengthExtractor);
  }

  private static String createWhereCondition(String group) {
    return "executor_group = '" + group + "'";
  }

  public boolean tick() {
    if (nextUpdate.isAfterNow()) {
      return false;
    }
    nextUpdate = now().plusSeconds(keepaliveIntervalSeconds);
    updateActiveTimestamp();
    return true;
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
      host = left(getLocalHost().getCanonicalHostName(), hostMaxLength);
      pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    } catch (UnknownHostException | NumberFormatException ex) {
      throw new RuntimeException("Failed to obtain host name and pid of running jvm", ex);
    }
    logger.info("Joining executor group {}", executorGroup);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value = { "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
          "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "findbugs does not trust jdbctemplate, sql is constant in practice")
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        String sql = "insert into nflow_executor(host, pid, executor_group, active, expires) values (?, ?, ?, current_timestamp, "
            + sqlVariants.currentTimePlusSeconds(timeoutSeconds) + ")";
        PreparedStatement p = con.prepareStatement(sql, new String[] { "id" });
        p.setString(1, host);
        p.setInt(2, pid);
        p.setString(3, executorGroup);
        return p;
      }
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  public void updateActiveTimestamp() {
    updateWithPreparedStatement("update nflow_executor set active=current_timestamp, expires="
        + sqlVariants.currentTimePlusSeconds(timeoutSeconds) + " where id = " + getExecutorId());
  }

  private void updateWithPreparedStatement(String sql) {
    // jdbc.update(sql) won't use prepared statements, this uses.
    jdbc.update(sql, (PreparedStatementSetter) null);
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

  public void markShutdown() {
    try {
      jdbc.update("update nflow_executor set expires=current_timestamp where executor_group = ? and id = ?", executorGroup,
          getExecutorId());
    } catch (DataAccessException e) {
      logger.warn("Failed to mark executor as expired", e);
    }
  }
}
