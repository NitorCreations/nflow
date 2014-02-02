package com.nitorcreations.nflow.engine.dao;

import static java.lang.System.getProperty;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource({"classpath:junit.properties"})
@EnableTransactionManagement
public class DaoTestConfiguration {
	
  @Bean
  public DataSource datasource() {
    SingleConnectionDataSource ds = new SingleConnectionDataSource();
    ds.setDriverClassName("org.h2.Driver");
    ds.setUrl(getProperty("db.url", "jdbc:h2:mem:test"));
    ds.setUsername("sa");
    ds.setPassword("");
    return ds;
  }
  
  @Bean
  public RepositoryDao repositoryDao(Environment env) {
    return new RepositoryDao(datasource(), env);
  }
  
}

