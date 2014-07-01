package com.nitorcreations.nflow.engine.internal.dao;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.nitorcreations.nflow.engine.internal.storage.db.H2DatabaseConfiguration;

@Configuration
@PropertySource({"classpath:junit.properties"})
@EnableTransactionManagement
@Import(H2DatabaseConfiguration.class)
public class DaoTestConfiguration {

  @Bean
  public WorkflowInstanceDao workflowInstanceDao(DataSource ds, Environment env) {
    return new WorkflowInstanceDao(ds, env);
  }

}

