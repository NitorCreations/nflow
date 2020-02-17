package io.nflow.engine.internal.dao;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.db.H2DatabaseConfiguration;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@PropertySource({ "classpath:junit.properties" })
@EnableTransactionManagement
@Import(H2DatabaseConfiguration.class)
public class DaoTestConfiguration {

  @Bean
  public WorkflowInstanceDao workflowInstanceDao(SQLVariants sqlVariants,
                                                 @NFlow JdbcTemplate nflowJdbcTemplate,
                                                 @NFlow TransactionTemplate transactionTemplate,
                                                 @NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate,
                                                 ExecutorDao executorDao,
                                                 WorkflowInstanceExecutor workflowInstanceExecutor,
                                                 WorkflowInstanceFactory workflowInstanceFactory,
                                                 Environment env) {
    return new WorkflowInstanceDao(sqlVariants,
            nflowJdbcTemplate,
            transactionTemplate,
            nflowNamedParameterJdbcTemplate,
            executorDao,
            workflowInstanceExecutor,
            workflowInstanceFactory,
            env);
  }

  @Bean
  public WorkflowDefinitionDao workflowDefinitionDao(SQLVariants sqlVariants,
                                                     @NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate,
                                                     @NFlow ObjectMapper nflowObjectMapper,
                                                     ExecutorDao executorDao) {
    return new WorkflowDefinitionDao(sqlVariants,
            nflowNamedParameterJdbcTemplate,
            nflowObjectMapper,
            executorDao);
  }

  @Bean
  public ExecutorDao executorDao(SQLVariants sqlVariants, @NFlow JdbcTemplate jdbcTemplate, Environment env) {
    return new ExecutorDao(sqlVariants, jdbcTemplate, env);
  }

  @Bean
  public StatisticsDao statisticsDao(@NFlow JdbcTemplate jdbcTemplate, ExecutorDao executorDao) {
    return new StatisticsDao(jdbcTemplate, executorDao);
  }

  @Bean
  public MaintenanceDao archiveDao(SQLVariants sqlVariants, @NFlow JdbcTemplate jdbcTemplate,
      @NFlow NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    return new MaintenanceDao(sqlVariants, jdbcTemplate, namedParameterJdbcTemplate);
  }

  @Bean
  public HealthCheckDao healthCheckDao(@NFlow JdbcTemplate jdbcTemplate) {
    return new HealthCheckDao(jdbcTemplate);
  }

  @Bean
  public TableMetadataChecker tableMetadataChecker(@NFlow JdbcTemplate jdbcTemplate) {
    return new TableMetadataChecker(jdbcTemplate);
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  @Bean
  @NFlow
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
    return mapper;
  }

  @Bean
  public WorkflowInstanceExecutor workflowInstanceExecutor() {
    return new WorkflowInstanceExecutor(10, 1, 5, 10, 10, new CustomizableThreadFactory("junit-"));
  }

  @Bean
  public WorkflowInstanceFactory workflowInstanceFactory() {
    return new WorkflowInstanceFactory(new ObjectStringMapper(objectMapper()));
  }

}
