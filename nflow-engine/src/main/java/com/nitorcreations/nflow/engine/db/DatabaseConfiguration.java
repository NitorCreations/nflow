package com.nitorcreations.nflow.engine.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.db.migrations.DatabaseSchemaMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public abstract class DatabaseConfiguration {
  private static final Logger logger = getLogger(DatabaseConfiguration.class);
  private final String dbType;

  @Inject
  private Environment env;

  protected DatabaseConfiguration(String dbType) {
    this.dbType = dbType;
  }

  @Bean(name = "nflow-datasource")
  public DataSource datasource() {
    String url = property("url");
    logger.info("Database connection to " + dbType + " using " + url);
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(property("driver"));
    config.addDataSourceProperty("url", url);
    config.addDataSourceProperty("user", property("user"));
    config.addDataSourceProperty("password", property("password"));
    config.setMaximumPoolSize(property("max_pool_size", Integer.class));
    return new HikariDataSource(config);
  }

  protected String property(String key) {
    return property(key, String.class);
  }

  protected <T> T property(String key, Class<T> type) {
    T val = env.getProperty("nflow.db." + key, type);
    if (val == null) {
      val = env.getProperty("nflow.db." + dbType + "." + key, type);
      if (val == null) {
        throw new IllegalStateException("required key [nflow.db." + key
            + "] not found");
      }
    }
    return val;
  }

  @Bean
  public DatabaseSchemaMigrator databaseMigrator() {
    return new DatabaseSchemaMigrator(dbType, datasource(), env);
  }

  @PostConstruct
  public void executeDatabaseMigrations() {
    if (!env.getRequiredProperty("nflow.db.create_on_startup", Boolean.class)) {
      return;
    }
    databaseMigrator().migrateDatabaseSchema();
  }

}
