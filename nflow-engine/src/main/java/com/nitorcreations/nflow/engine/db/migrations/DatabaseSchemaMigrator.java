package com.nitorcreations.nflow.engine.db.migrations;

import java.sql.Connection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class DatabaseSchemaMigrator {
  static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrator.class);

  private final String dbType;
  private final DataSource dataSource;

  public DatabaseSchemaMigrator(String dbType, DataSource dataSource, Environment env) {
    this.dbType = dbType;
    this.dataSource = dataSource;
  }

  public void migrateDatabaseSchema() {
    Flyway flyway = createFlyway();
    flyway.migrate();
  }

  private Flyway createFlyway() {
    Flyway flyway = new Flyway();
    flyway.setInitVersion(MigrationVersion.fromVersion("0"));
    flyway.setTable("nflow_schema_version");
    flyway.setDataSource(dataSource);
    flyway.setLocations("classpath:/db/migration/" + dbType);
    FlywayCallback loggingCallback = new AbstractFlywayCallback() {
      @Override
      public void beforeMigrate(Connection connection) {
        log.info("Starting to migrate scripts");
      }

      @Override
      public void beforeEachMigrate(Connection connection, MigrationInfo info) {
        log.info("Starting to migrate script {}", info.getScript());
      }

      @Override
      public void afterEachMigrate(Connection connection, MigrationInfo info) {
        log.info("Finished migrating script {}", info.getScript());
      }
      @Override
      public void afterMigrate(Connection connection) {
        log.info("Finished migrating scripts");
      }
    };
    flyway.setCallbacks(loggingCallback);
    // create nflow_schema_version table if missing
    flyway.setInitOnMigrate(true);
    return flyway;
  }
}
