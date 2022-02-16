package io.nflow.engine.config.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

import org.slf4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;

/**
 * Base class for different database configurations.
 */
public abstract class DatabaseConfiguration {

  /**
   * Name of the nFlow database initializer bean.
   */
  public static final String NFLOW_DATABASE_INITIALIZER = "nflowDatabaseInitializer";

  private static final Logger logger = getLogger(DatabaseConfiguration.class);

  private final String dbType;

  /**
   * Creates a new configuration with given database type.
   * @param dbType Defines the database creation script and configuration properties to be used.
   */
  protected DatabaseConfiguration(String dbType) {
    this.dbType = dbType;
  }

  /**
   * Return the type of the database.
   * @return Type of the database.
   */
  public String getDbType() {
    return dbType;
  }

  public DataSource nflowDatasource(NFlowConfiguration config, Object metricRegistry) {
    String url = property(config, "url");
    logger.info("Database connection to {} using {}", dbType, url);
    HikariConfig pool = new HikariConfig();
    pool.setPoolName("nflow");
    pool.setDriverClassName(property(config, "driver"));
    pool.setJdbcUrl(url);
    pool.setUsername(property(config, "user"));
    pool.setPassword(property(config, "password"));
    pool.setMaximumPoolSize(property(config, "max_pool_size", Integer.class));
    pool.setIdleTimeout(property(config, "idle_timeout_seconds", Long.class) * 1000);
    pool.setAutoCommit(true);
    if (metricRegistry != null) {
      pool.setMetricRegistry(metricRegistry);
    }
    return new HikariDataSource(pool);
  }

  /**
   * Get a database configuration string property from the environment, or if the generic property is not defined, the property
   * based on the database type.
   * @param config The NFlow configuration.
   * @param key The property key.
   * @return The property value.
   */
  protected String property(NFlowConfiguration config, String key) {
    return property(config, key, String.class);
  }

  /**
   * Get the database configuration property of given type from the environment, or if the generic property is not defined, the
   * property based on the database type.
   * @param <T> The Property value type.
   * @param config The NFlow configuration.
   * @param key The property key.
   * @param type The property value type.
   * @return The property value.
   */
  protected <T> T property(NFlowConfiguration config, String key, Class<T> type) {
    T val = config.getProperty("nflow.db." + key, type, null);
    if (val == null) {
      val = config.getProperty("nflow.db." + dbType + "." + key, type, null);
      if (val == null) {
        throw new IllegalStateException("required key [nflow.db." + key + "] not found");
      }
    }
    return val;
  }

  /**
   * Creates the nFlow database initializer.
   * @param dataSource The nFlow datasource.
   * @param config The NFlow configuration.
   * @return The database initializer.
   */
  public DatabaseInitializer nflowDatabaseInitializer(DataSource dataSource, NFlowConfiguration config) {
    return new DatabaseInitializer(dbType, dataSource, config, ";");
  }

  /**
   * Checks that the database is configured as nFlow expects.
   * @param config The NFlow configuration.
   * @param dataSource The nFlow datasource.
   */
  @SuppressFBWarnings(value = "ACEM_ABSTRACT_CLASS_EMPTY_METHODS", justification = "Most databases do not check database configuration")
  protected void checkDatabaseConfiguration(NFlowConfiguration config, DataSource dataSource) {
    // no common checks for all databases
  }

  /**
   * Creates the SQL variants for the database.
   *
   * @param config The NFlow configuration.
   * @return SQL variants optimized for the database.
   */
  public abstract SQLVariants sqlVariants(NFlowConfiguration config);
}
