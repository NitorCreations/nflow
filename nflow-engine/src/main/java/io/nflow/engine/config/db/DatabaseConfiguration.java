package io.nflow.engine.config.db;

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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;

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
   * Creates the datasource bean for nFlow.
   * @param env The Spring environment for getting the configuration property values.
   * @param appCtx The application context for searching Metrics registry bean.
   * @return The datasource for nFlow.
   */
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
    config.setIdleTimeout(property(env, "idle_timeout_seconds", Long.class) * 1000);
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
    } catch (@SuppressWarnings("unused") ClassNotFoundException | NoSuchBeanDefinitionException e) {
      // ignored - metrics is an optional dependency
    }
  }

  /**
   * Creates a JDBC template using nFlow datasource.
   * @param nflowDataSource The nFlow datasource.
   * @return A JDBC template.
   */
  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public JdbcTemplate nflowJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new JdbcTemplate(nflowDataSource);
  }

  /**
   * Creates a named parameter JDBC template using nFlow datasource.
   * @param nflowDataSource The nFlow datasource.
   * @return A named parameter JDBC template.
   */
  @Bean
  @NFlow
  @Scope(SCOPE_PROTOTYPE)
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate(@NFlow DataSource nflowDataSource) {
    return new NamedParameterJdbcTemplate(nflowDataSource);
  }

  /**
   * Creates a transaction template.
   * @param platformTransactionManager Transaction manager to be used.
   * @return A transaction template.
   */
  @Bean
  @NFlow
  public TransactionTemplate nflowTransactionTemplate(PlatformTransactionManager platformTransactionManager) {
    return new TransactionTemplate(platformTransactionManager);
  }

  /**
   * Get a database configuration string property from the environment, or if the generic property is not defined, the property
   * based on the database type.
   * @param env The Spring environment.
   * @param key The property key.
   * @return The property value.
   */
  protected String property(Environment env, String key) {
    return property(env, key, String.class);
  }

  /**
   * Get the database configuration property of given type from the environment, or if the generic property is not defined, the
   * property based on the database type.
   * @param <T> The Property value type.
   * @param env The Spring environment.
   * @param key The property key.
   * @param type The property value type.
   * @return The property value.
   */
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

  /**
   * Creates the nFlow database initializer.
   * @param dataSource The nFlow datasource.
   * @param env The Spring environment.
   * @return The database initializer.
   */
  @Bean(name = NFLOW_DATABASE_INITIALIZER)
  @NFlow
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource dataSource, Environment env) {
    return new DatabaseInitializer(dbType, dataSource, env);
  }

}
