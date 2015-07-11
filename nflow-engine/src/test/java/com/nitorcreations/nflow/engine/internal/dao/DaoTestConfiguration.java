package com.nitorcreations.nflow.engine.internal.dao;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.storage.db.H2DatabaseConfiguration;
import com.nitorcreations.nflow.engine.internal.storage.db.H2DatabaseConfiguration.H2SQLVariants;

@Configuration
@PropertySource({"classpath:junit.properties"})
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
    dao.setSQLVariants(new H2SQLVariants());
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
}

