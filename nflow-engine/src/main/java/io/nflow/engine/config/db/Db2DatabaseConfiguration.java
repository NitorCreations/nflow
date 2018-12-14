package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.DB2;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;

import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Configuration for DB2 database. Note: tested only using DB2 Express-C (Docker: ibmcom/db2express-c).
 * DB2 database must be configured to UTC time zone, otherwise nFlow will fail to function correctly.
 */
@Profile(DB2)
@Configuration
public class Db2DatabaseConfiguration extends DatabaseConfiguration {

  /**
   * Create a new instance.
   */
  public Db2DatabaseConfiguration() {
    super("db2");
  }

  /**
   * Creates the SQL variants for DB2.
   * @param env The Spring environment for getting the configuration property values.
   * @return SQL variants optimized for DB2.
   */
  @Bean
  public SQLVariants sqlVariants(Environment env) {
    return new Db2SQLVariants(property(env, "timezone"));
  }

  /**
   * SQL variants optimized for DB2.
   */
  public static class Db2SQLVariants implements SQLVariants {

    private final ZoneId dbTimeZoneId;

    public Db2SQLVariants(String dbTimeZoneIdStr) {
      dbTimeZoneId = ZoneId.of(dbTimeZoneIdStr);
    }

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + " + seconds + " SECONDS";
    }

    /**
     * Returns false as DB2 does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as DB2 does not support updateable CTEs.
     */
    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    @Override
    public String nextActivationUpdate() {
      return "(case " //
              + "when ? is null then null " //
              + "when external_next_activation is null then ? " //
              + "else least(?, external_next_activation) end)";
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
    public String limit(String query, long limit) {
      // note: limit must be a number, because NamedJdbcTemplate does not set variables (e.g. :limit) here
      return query + " fetch first " + limit + " rows only";
    }

    /**
     * Returns the SQL type for long text.
     */
    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }

    /**
     * Returns true as DB2 Express-C supports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }

    @Override
    public Object getTimestamp(ResultSet rs, String columnName) throws SQLException {
      return Optional.ofNullable(rs.getTimestamp(columnName))
          .map(ts -> new Timestamp(ts.getTime() + timeZoneMismatchInMillis()))
          .orElse(null);
    }

    @Override
    public DateTime getDateTime(ResultSet rs, String columnName) throws SQLException {
      return Optional.ofNullable(rs.getTimestamp(columnName))
          .map(ts -> new DateTime(ts.getTime() + timeZoneMismatchInMillis()))
          .orElse(null);
    }

    @Override
    public void setDateTime(PreparedStatement ps, int columnNumber, DateTime timestamp) throws SQLException {
      ps.setTimestamp(columnNumber, toTimestamp(timestamp), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }

    @Override
    public Object toTimestampObject(DateTime timestamp) {
      return Optional.ofNullable(timestamp)
          .map(ts -> new Timestamp(timestamp.getMillis() - timeZoneMismatchInMillis()))
          .orElse(null);
    }

    @Override
    public Object tuneTimestampForDb(Object timestamp) {
      return new Timestamp(((Timestamp)timestamp).getTime() - timeZoneMismatchInMillis());
    }

    private long timeZoneMismatchInMillis() {
      long now = System.currentTimeMillis();
      return TimeZone.getDefault().getOffset(now) - TimeZone.getTimeZone(dbTimeZoneId).getOffset(now);
    }
  }
}
