package io.nflow.engine.config.db;

import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import static io.nflow.engine.config.Profiles.SQLSERVER;
import static io.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static java.lang.Class.*;

/**
 * Configuration for SQL Server database.
 */
@Profile(SQLSERVER)
@Configuration
public class SqlServerDatabaseConfiguration extends DatabaseConfiguration {

  /**
   * Create a new instance.
   */
  public SqlServerDatabaseConfiguration() {
    super("sqlserver");
  }

  /**
   * Creates the SQL variants for SQL Server database.
   * @return SQL variants optimized for SQL Server.
   */
  @Bean
  public SQLVariants sqlVariants() {
    return new SQLServerVariants();
  }

  /**
   * SQL variants optimized for SQL Server.
   */
  public static class SQLServerVariants implements SQLVariants {

    private static final Method getDateTimeOffsetMethod;
    private static final Method setDateTimeOffsetMethod;
    private static final Method getTimestampMethod;
    private static final Method createDateTimeOffsetMethod;

    private static final Class<?> sqlServerDateTimeOffset;
    private static final Class<?> sqlServerResultSet;
    private static final Class<?> sqlServerPreparedStatement;

    static {
      try {
        sqlServerDateTimeOffset = forName("microsoft.sql.DateTimeOffset");
        sqlServerResultSet = forName("com.microsoft.sqlserver.jdbc.ISQLServerResultSet42");
        sqlServerPreparedStatement = forName("com.microsoft.sqlserver.jdbc.ISQLServerPreparedStatement42");
        getDateTimeOffsetMethod = sqlServerResultSet.getMethod("getDateTimeOffset", String.class);
        setDateTimeOffsetMethod = sqlServerPreparedStatement.getMethod("setDateTimeOffset", Integer.TYPE, sqlServerDateTimeOffset);
        getTimestampMethod = sqlServerDateTimeOffset.getMethod("getTimestamp");
        createDateTimeOffsetMethod = sqlServerDateTimeOffset.getMethod("valueOf", Timestamp.class, Integer.TYPE);
      } catch (Exception e) {
        throw new RuntimeException("Could not find required getDateTimeOffset method from sqlserver jdbc driver", e);
      }
    }

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "dateadd(ss, " + seconds + ", current_timestamp)";
    }

    /**
     * Returns false as SQL Server does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as SQL Server does not support updateable CTEs.
     */
    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }


    @Override
    public String dateLtEqDiff(String next_activation, String current_timestamp) {
      return "datediff_big(ms, " + next_activation + ", " + current_timestamp + ") >= 0";
    }

    @Override
    public Object getTimestamp(ResultSet rs, String columnName) throws SQLException {
      try {
        return getDateTimeOffsetMethod.invoke(rs.unwrap(sqlServerResultSet), columnName);
      } catch (Exception e) {
        throw new SQLException("Failed to invoke getDateTimeOffset on ResultSet ", e);
      }
    }

    @Override
    public DateTime getDateTime(ResultSet rs, String columnName) throws SQLException {
      try {
        Object dateTimeOffset = getDateTimeOffsetMethod.invoke(rs.unwrap(sqlServerResultSet), columnName);
        if (dateTimeOffset == null) {
          return null;
        }
        return toDateTime((Timestamp) getTimestampMethod.invoke(dateTimeOffset));
      } catch (Exception e) {
        throw new SQLException("Failed to invoke getDateTimeOffset on ResultSet", e);
      }
    }

    @Override
    public void setDateTime(PreparedStatement ps, int columnNumber, DateTime timestamp) throws SQLException {
      try {
        setDateTimeOffsetMethod.invoke(ps.unwrap(sqlServerPreparedStatement), columnNumber, toTimestampObject(timestamp));
      } catch (Exception e) {
        throw new SQLException("Failed to invoke setDateTimeOffset on PreparedStatement", e);
      }
    }

    @Override
    public Object toTimestampObject(DateTime timestamp) {
      if (timestamp == null) {
        return null;
      }
      try {
        return createDateTimeOffsetMethod.invoke(null, toTimestamp(timestamp), 0);
      } catch (Exception e) {
        throw new RuntimeException("Failed to create DateTimeOffset instance", e);
      }
    }

    /**
     * Returns SQL representing the next activation time of the workflow instance.
     */
    @Override
    public String nextActivationUpdate() {
      return "(case "
              + "when ? is null then null "
              + "else iif(datediff_big(ms, ?, external_next_activation) > 0, external_next_activation, ?) end)";
    }

    /**
     * Returns the SQL representation for given workflow instance status.
     */
    @Override
    public String workflowStatus(WorkflowInstanceStatus status) {
      return "'" + status.name() + "'";
    }

    /**
     * Returns SQL representing the workflow instance status parameter.
     */
    @Override
    public String workflowStatus() {
      return "?";
    }

    /**
     * Returns SQL representing the action type parameter.
     */
    @Override
    public String actionType() {
      return "?";
    }

    /**
     * Returns string for casting value to text.
     */
    @Override
    public String castToText() {
      return "";
    }

    /**
     * Returns SQL for a query with a limit of results.
     */
    @Override
    public String limit(String query, String limit) {
      int idx = query.indexOf("select ");
      return query.substring(0, idx + 7) + "top(" + limit + ") " + query.substring(idx + 7);
    }

    /**
     * Returns the SQL type for long text.
     */
    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }

    /**
     * Returns true as SQL Server suppports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }
  }
}
