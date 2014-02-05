package com.nitorcreations.nflow.engine.config;

import static java.lang.System.getProperty;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public DataSource datasource() {
    SingleConnectionDataSource ds = new SingleConnectionDataSource();
    ds.setDriverClassName("org.h2.Driver");
    ds.setUrl(getProperty("db.url", "jdbc:h2:mem:test;TRACE_LEVEL_FILE=4"));
    ds.setUsername("sa");
    ds.setPassword("");
    return ds;
  }
  
  @Bean
  public DatabaseInitializer dbInitializer(DataSource ds) {
    return new DatabaseInitializer(ds);
  }
  
}
