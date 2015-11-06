package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
  public DataSource nflowDatasource(Environment env, BeanFactory appCtx) {
    String url = property(env, "url");
    logger.info("Database connection to {} using {}", dbType, url);
    HikariConfig config = new HikariConfig();
    config.setPoolName("nflow");
    config.setDataSourceClassName(property(env, "driver"));
    config.addDataSourceProperty("url", url);
    config.setUsername(property(env, "user"));
    config.setPassword(property(env, "password"));
    config.setMaximumPoolSize(property(env, "max_pool_size", Integer.class));
    config.setIdleTimeout(property(env, "idle_timeout_seconds", Integer.class) * 1000);
    config.setAutoCommit(true);
    setMetricRegistryIfBeanFoundOnClassPath(config, appCtx);
    return new HikariDataSource(config);
  }

  private void setMetricRegistryIfBeanFoundOnClassPath(HikariConfig config, BeanFactory appCtx) {
    try {
      Class<?> metricClass = Class.forName("com.codahale.metrics.MetricRegistry");
      Object metricRegistry = appCtx.getBean(metricClass);
      if (metricRegistry != null) {
        config.setMetricRegistry(metricRegistry);
      }
    } catch (ClassNotFoundException | NoSuchBeanDefinitionException e) {
      // ignored - metrics is an optional dependency
    }
  }

  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public JdbcTemplate nflowJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new JdbcTemplate(nflowDataSource);
  }

  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new NamedParameterJdbcTemplate(nflowDataSource);
  }

  @Bean
  @NFlow
  public TransactionTemplate nflowTransactionTemplate(PlatformTransactionManager platformTransactionManager) {
    return new TransactionTemplate(platformTransactionManager);
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
