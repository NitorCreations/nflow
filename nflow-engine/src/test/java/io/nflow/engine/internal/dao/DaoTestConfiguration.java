package io.nflow.engine.internal.dao;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.H2DatabaseConfiguration;
import io.nflow.engine.internal.storage.db.H2DatabaseConfiguration.H2SQLVariants;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@PropertySource({ "classpath:junit.properties" })
@EnableTransactionManagement
@Import(H2DatabaseConfiguration.class)
public class DaoTestConfiguration {

  @Bean
  public WorkflowInstanceDao workflowInstanceDao() {
    return new WorkflowInstanceDao();
  }

  @Bean
  public WorkflowDefinitionDao workflowDefinitionDao() {
    return new WorkflowDefinitionDao();
  }

  @Bean
  public ExecutorDao executorDao(Environment env) {
    ExecutorDao dao = new ExecutorDao();
    dao.setSqlVariants(new H2SQLVariants());
    dao.setEnvironment(env);
    return dao;
  }

  @Bean
  public StatisticsDao statisticsDao() {
    return new StatisticsDao();
  }

  @Bean
  public ArchiveDao archiveDao() {
    return new ArchiveDao();
  }

  @Bean
  public HealthCheckDao healthCheckDao() {
    return new HealthCheckDao();
  }

  @Bean
  public TableMetadataChecker tableMetadataChecker() {
    return new TableMetadataChecker();
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
