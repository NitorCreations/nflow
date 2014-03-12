package com.nitorcreations.nflow.engine.config;

import static java.lang.System.getProperty;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jolbox.bonecp.BoneCPDataSource;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public DataSource datasource() {
    BoneCPDataSource ds = new BoneCPDataSource();
    ds.setDriverClass("org.h2.Driver");
    ds.setJdbcUrl(getProperty("db.url", "jdbc:h2:mem:test;TRACE_LEVEL_FILE=4"));
    ds.setUsername("sa");
    ds.setPassword("");
    ds.setPartitionCount(4);
    ds.setMaxConnectionsPerPartition(25);
    return ds;
  }

  @Bean
  public DatabaseInitializer dbInitializer(DataSource ds) {
    return new DatabaseInitializer(ds);
  }

}
