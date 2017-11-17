package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.MYSQL;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.split;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Configuration for MySQL database.
 */
@Profile(MYSQL)
@Configuration
public class MysqlDatabaseConfiguration extends DatabaseConfiguration {
  private static final Logger logger = getLogger(MysqlDatabaseConfiguration.class);

  /**
   * Create a new instance.
   */
  public MysqlDatabaseConfiguration() {
    super("mysql");
  }

  /**
   * Creates the nFlow database initializer. Selects correct database creation script based on database version.
   * @param nflowDataSource The nFlow datasource.
   * @param env The Spring environment.
   * @return The database initializer.
   */
  @Bean
  @Override
  @SuppressFBWarnings(value = { "CLI_CONSTANT_LIST_INDEX", "WEM_WEAK_EXCEPTION_MESSAGING" }, //
      justification = "extracting major and minor version from splitted string, exception message is ok")
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    String dbType = "mysql";
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      String databaseProductVersion = meta.getDatabaseProductVersion();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("MySQL {}.{}, product version {}", majorVersion, minorVersion, databaseProductVersion);
      if (databaseProductVersion.contains("MariaDB")) {
        if (databaseProductVersion.startsWith("5.5.5-")) {
          databaseProductVersion = databaseProductVersion.substring(6);
        }
        String[] versions = split(databaseProductVersion, ".-");
        if (parseInt(versions[0]) <= 5 && parseInt(versions[1]) <= 5) {
          dbType += ".legacy";
        }
      } else if (majorVersion <= 5 && minorVersion <= 5) {
        dbType += ".legacy";
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain mysql version", e);
    }
    return new DatabaseInitializer(dbType, nflowDataSource, env);
  }

  /**
   * Creates the SQL variants for MySQL database.
   * @return SQL variants optimized for MySQL.
   */
  @Bean
  public SQLVariants sqlVariants() {
    return new MySQLVariants();
  }

  /**
   * SQL variants optimized for MySQL.
   */
  public static class MySQLVariants implements SQLVariants {

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "from_unixtime(unix_timestamp() + " + seconds + ")";
    }

    /**
     * Returns false as MySQL does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as MySQL does not support updateable CTEs.
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
      return "(case "
          + "when ? is null then null "
          + "when external_next_activation is null then ? "
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
     * Returns empty string as casting to text is not needed in MySQL.
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
      return query + " limit " + limit;
    }

    /**
     * Returns the SQL type for long text.
     */
    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }

    /**
     * Returns true as MySQL suppports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }
  }
}
