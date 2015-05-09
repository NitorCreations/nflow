package com.nitorcreations.nflow.engine.internal.storage.db;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.split;
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

import com.nitorcreations.nflow.engine.internal.config.NFlow;

@Profile("nflow.db.mysql")
@Configuration
public class MysqlDatabaseConfiguration extends DatabaseConfiguration {
  private static final Logger logger = getLogger(MysqlDatabaseConfiguration.class);

  public MysqlDatabaseConfiguration() {
    super("mysql");
  }

  @Bean
  @Override
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    String dbType = "mysql";
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      String databaseProductVersion = meta.getDatabaseProductVersion();
      logger.info("MySQL {}.{}, product version {}", meta.getDatabaseMajorVersion(), meta.getDatabaseMinorVersion(), databaseProductVersion);
      if (databaseProductVersion.contains("MariaDB")) {
        if (databaseProductVersion.startsWith("5.5.5-")) {
          databaseProductVersion = databaseProductVersion.substring(6);
        }
        String[] versions = split(databaseProductVersion, ".-");
        if (parseInt(versions[0]) <= 5 && parseInt(versions[1]) <= 5) {
          dbType += ".legacy";
        }
      } else if (meta.getDatabaseMajorVersion() <=5 && meta.getDatabaseMinorVersion() <= 5) {
        dbType += ".legacy";
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain mysql version", e);
    }
    return new DatabaseInitializer(dbType, nflowDataSource, env);
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

    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    @Override
    public String castToEnumType(String variable, String type) {
      return variable;
    }

    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    @Override
    public String least(String value1, String value2) {
      return format("(case " +
              "when %1$s is null then %2$s " +
              "when %2$s is null then %1$s " +
              "when %1$s < %2$s then %1$s " +
              "else %2$s end)",
              value1, value2);
    }

    @Override
    public String least1Param(String value1, String value2) {
      return least(value1, value2);
    }
  }
}
