package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.MARIADB;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

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
import io.nflow.engine.internal.storage.db.MySQLVariants;
import io.nflow.engine.internal.storage.db.SQLVariants;

/**
 * Configuration for MariaDB database.
 */
@Profile(MARIADB)
@Configuration
public class MariadbDatabaseConfiguration extends DatabaseConfiguration {
  private static final Logger logger = getLogger(MariadbDatabaseConfiguration.class);

  /**
   * Create a new instance.
   */
  public MariadbDatabaseConfiguration() {
    super("mariadb");
  }

  /**
   * Creates the nFlow database initializer. Selects correct database creation script based on database version.
   * @param nflowDataSource The nFlow datasource.
   * @param env The Spring environment.
   * @return The database initializer.
   */
  @Bean
  @Override
  @SuppressFBWarnings(value = { "WEM_WEAK_EXCEPTION_MESSAGING", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
      justification = "exception message is ok, null-check in try-catch")
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    String scriptPrefix = "mariadb";
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      String databaseProductVersion = meta.getDatabaseProductVersion();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("MariaDB {}.{}, product version {}", majorVersion, minorVersion, databaseProductVersion);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain MariaDB version", e);
    }
    return new DatabaseInitializer(scriptPrefix, nflowDataSource, env, ";");
  }

  /**
   * {@inheritDoc}
   */
  @Bean
  @Override
  public SQLVariants sqlVariants(Environment env) {
    return new MySQLVariants();
  }
}
