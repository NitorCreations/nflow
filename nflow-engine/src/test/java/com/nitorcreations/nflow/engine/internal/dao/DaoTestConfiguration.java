package com.nitorcreations.nflow.engine.internal.dao;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
  public PlatformTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

}

