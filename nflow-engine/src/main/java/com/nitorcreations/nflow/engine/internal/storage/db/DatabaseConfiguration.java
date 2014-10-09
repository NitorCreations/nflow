package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Named;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public abstract class DatabaseConfiguration {
  public static final String NFLOW_JDBC = "nflowJdbc";
  public static final String NFLOW_NAMED_JDBC = "nflowNamedJdbc";
  public static final String NFLOW_DATASOURCE = "nflowDatasource";
  private static final Logger logger = getLogger(DatabaseConfiguration.class);
  private final String dbType;

  protected DatabaseConfiguration(String dbType) {
    this.dbType = dbType;
  }

  @Bean(name = NFLOW_DATASOURCE)
  public DataSource datasource(Environment env) {
    String url = property(env, "url");
    logger.info("Database connection to {} using {}", dbType, url);
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(property(env, "driver"));
    config.addDataSourceProperty("url", url);
    config.addDataSourceProperty("user", property(env, "user"));
    config.addDataSourceProperty("password", property(env, "password"));
    config.setMaximumPoolSize(property(env, "max_pool_size", Integer.class));
    return new HikariDataSource(config);
  }

  @Bean(name = NFLOW_JDBC)
  public JdbcTemplate jdbcTemplate(Environment env) {
    return new JdbcTemplate(datasource(env));
  }

  @Bean(name = NFLOW_NAMED_JDBC)
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(Environment env) {
    return new NamedParameterJdbcTemplate(datasource(env));
  }

  protected String property(Environment env, String key) {
    return property(env, key, String.class);
  }

  protected <T> T property(Environment env, String key, Class<T> type) {
    T val = env.getProperty("nflow.db." + key, type);
    if (val == null) {
      val = env.getProperty("nflow.db." + dbType + "." + key, type);
      if (val == null) {
        throw new IllegalStateException("required key [nflow.db." + key + "] not found");
      }
    }
    return val;
  }

  @Bean(name = "nflowDatabaseInitializer")
  public DatabaseInitializer nflowDatabaseInitializer(@Named(NFLOW_DATASOURCE) DataSource dataSource, Environment env) {
    return new DatabaseInitializer(dbType, dataSource, env);
  }
}
