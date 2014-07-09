package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.inject.Named;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceUtils;

@Profile("nflow.db.mysql")
@Configuration
public class MysqlDatabaseConfiguration extends DatabaseConfiguration {
  private static final Logger logger = getLogger(MysqlDatabaseConfiguration.class);

  public MysqlDatabaseConfiguration() {
    super("mysql");
  }

  @Bean(name="nflowDatabaseInitializer")
  @Override
  public DatabaseInitializer nflowDatabaseInitializer(@Named("nflow-datasource") DataSource dataSource, Environment env) {
    String dbType = "mysql";
    try (Connection c = DataSourceUtils.getConnection(dataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      logger.info("MySQL {}.{}, product version {}", meta.getDatabaseMajorVersion(), meta.getDatabaseMinorVersion(), meta.getDatabaseProductVersion());
      if (!meta.getDatabaseProductVersion().contains("MariaDB") && meta.getDatabaseMajorVersion() <=5 && meta.getDatabaseMinorVersion() <= 5) {
        dbType += ".legacy";
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain mysql version");
    }
    return new DatabaseInitializer(dbType, dataSource, env);
  }


  @Bean
  public SQLVariants sqlVariants() {
    return new MySQLVariants();
  }

  public static class MySQLVariants implements SQLVariants {
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "date_add(current_timestamp, interval " + seconds + " second)";
    }
  }
}
