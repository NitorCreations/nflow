package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.firstColumnLengthExtractor;
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

import javax.inject.Inject;
import javax.inject.Singleton;

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
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.executor.WorkflowExecutor;

/**
 * Use setter injection because constructor injection may not work when nFlow is used in some legacy systems.
 */
@Component
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
@Singleton
public class ExecutorDao {

  private static final Logger logger = getLogger(ExecutorDao.class);

  private final JdbcTemplate jdbc;
  final SQLVariants sqlVariants;
  private final int keepaliveIntervalSeconds;
  private DateTime nextUpdate = now();
  final String executorGroup;
  private final String executorGroupCondition;
  final int timeoutSeconds;
  private int executorId = -1;
  private final int hostMaxLength;

  @Inject
  public ExecutorDao(SQLVariants sqlVariants, @NFlow JdbcTemplate nflowJdbcTemplate, Environment env) {
    this.sqlVariants = sqlVariants;
    this.jdbc = nflowJdbcTemplate;
    this.executorGroup = trimToNull(env.getRequiredProperty("nflow.executor.group"));
    this.executorGroupCondition = createWhereCondition(executorGroup);
    this.timeoutSeconds = env.getRequiredProperty("nflow.executor.timeout.seconds", Integer.class);
    this.keepaliveIntervalSeconds = env.getRequiredProperty("nflow.executor.keepalive.seconds", Integer.class);
    // In one deployment, FirstColumnLengthExtractor returned 0 column length (H2), so allow explicit length setting.
    this.hostMaxLength = env.getProperty("nflow.executor.host.length", Integer.class, -1);
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
      int hostNameMaxLength = hostMaxLength == -1
          ? jdbc.query("select host from nflow_executor where 1 = 0", firstColumnLengthExtractor)
          : hostMaxLength;
      executorId = allocateExecutorId(hostNameMaxLength);
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

  public boolean isAutoCommitEnabled() {
    return jdbc.execute(Connection::getAutoCommit);
  }

  @SuppressFBWarnings(value = { "MDM_INETADDRESS_GETLOCALHOST", "WEM_WEAK_EXCEPTION_MESSAGING" }, //
      justification = "localhost is used for getting host name only, exception message is fine")
  private int allocateExecutorId(int hostNameMaxLength) {
    final String host;
    final int pid;
    try {
      host = left(getLocalHost().getCanonicalHostName(), hostNameMaxLength);
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
    int allocatedExecutorId = keyHolder.getKey().intValue();
    logger.info("Joined executor group {} as executor {} running on host {} with process id {}.", executorGroup,
        allocatedExecutorId, host, pid);
    return allocatedExecutorId;
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
        DateTime started = sqlVariants.getDateTime(rs, "started");
        DateTime active = sqlVariants.getDateTime(rs, "active");
        DateTime expires = sqlVariants.getDateTime(rs, "expires");
        DateTime stopped = sqlVariants.getDateTime(rs, "stopped");
        return new WorkflowExecutor(id, host, pid, executorGroup, started, active, expires, stopped);
      }
    }, executorGroup);
  }

  public void markShutdown() {
    try {
      jdbc.update("update nflow_executor " + "set expires=current_timestamp, stopped=current_timestamp "
          + "where executor_group = ? and id = ?", executorGroup, getExecutorId());
    } catch (DataAccessException e) {
      logger.warn("Failed to mark executor as expired", e);
    }
  }
}
