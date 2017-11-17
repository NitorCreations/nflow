package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.ORACLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Configuration for Oracle database.
 */
@Profile(ORACLE)
@Configuration
public class OracleDatabaseConfiguration extends DatabaseConfiguration {

  private static final Logger logger = getLogger(OracleDatabaseConfiguration.class);
  public static final String DB_TYPE_ORACLE = "oracle";
  private boolean useBatchUpdate;

  /**
   * Create a new instance.
   */
  public OracleDatabaseConfiguration() {
    super(DB_TYPE_ORACLE);
  }

  /**
   * Creates the nFlow database initializer.
   * @param nflowDataSource The nFlow datasource.
   * @param env The Spring environment.
   * @return The database initializer.
   */
  @Bean
  @Override
  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "exception message is ok")
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("Oracle {}.{}, product version {}", majorVersion, minorVersion, meta.getDatabaseProductVersion());
      useBatchUpdate = (majorVersion > 12 || (majorVersion == 12 && minorVersion >= 1));
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain oracle version", e);
    }
    return new DatabaseInitializer(DB_TYPE_ORACLE, nflowDataSource, env);
  }

  /**
   * Creates the SQL variants for Oracle database.
   * @return SQL variants optimized for Oracle.
   */
  @Bean
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public SQLVariants sqlVariants() {
    return new OracleSqlVariants(useBatchUpdate);
  }

  /**
   * SQL variants optimized for Oracle.
   */
  public static class OracleSqlVariants implements SQLVariants {

    private final boolean useBatchUpdate;

    /**
     * Create a new instance.
     * @param useBatchUpdate True for database versions 12.1 or newer.
     */
    public OracleSqlVariants(boolean useBatchUpdate) {
      this.useBatchUpdate = useBatchUpdate;
    }

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + interval '" + seconds + "' second";
    }

    /**
     * Returns false as Oracle does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as Oracle does not support updateable CTEs.
     */
    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    /**
     * Returns SQL representing the next activation time of the workflow instance.
     */
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
     * Returns empty string as casting to text is not needed in Oracle.
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
      return "select * from (" + query + ") where rownum <= " + limit;
    }

    /**
     * Returns the SQL type for long text.
     */
    @Override
    public int longTextType() {
      return Types.CLOB;
    }

    /**
     * Returns true for database versions 12.1 or newer.
     */
    @Override
    public boolean useBatchUpdate() {
      return useBatchUpdate;
    }
  }
}
