package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public abstract class DatabaseConfiguration {
  public static final String NFLOW_DATABASE_INITIALIZER = "nflowDatabaseInitializer";
  private static final Logger logger = getLogger(DatabaseConfiguration.class);
  private final String dbType;

  protected DatabaseConfiguration(String dbType) {
    this.dbType = dbType;
  }

  @Bean
  @NFlow
  public DataSource nflowDatasource(Environment env) {
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

  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  public JdbcTemplate nflowJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new JdbcTemplate(nflowDataSource);
  }

  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  public NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new NamedParameterJdbcTemplate(nflowDataSource);
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

  @Bean(name = NFLOW_DATABASE_INITIALIZER)
  @NFlow
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource dataSource, Environment env) {
    return new DatabaseInitializer(dbType, dataSource, env);
  }
}
