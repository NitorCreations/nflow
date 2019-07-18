package io.nflow.engine.internal.storage.db;

import static io.nflow.engine.internal.config.Profiles.MARIADB;
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
import io.nflow.engine.internal.config.NFlow;

@Profile(MARIADB)
@Configuration
public class MariadbDatabaseConfiguration extends MysqlDatabaseConfiguration {
  private static final Logger logger = getLogger(MariadbDatabaseConfiguration.class);

  public MariadbDatabaseConfiguration() {
    super("mariadb");
  }

  @Bean
  @Override
  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "exception message is ok")
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      String databaseProductVersion = meta.getDatabaseProductVersion();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("MariaDB {}.{}, product version {}", majorVersion, minorVersion, databaseProductVersion);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain mariadb version", e);
    }
    return new DatabaseInitializer("mysql", nflowDataSource, env);
  }

}
