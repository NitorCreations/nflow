package com.nitorcreations.nflow.engine.config;

import static java.lang.System.getProperty;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public DataSource datasource() throws ClassNotFoundException {
    HikariDataSource ds = new HikariDataSource();
    Class.forName("org.h2.Driver");
    ds.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
    ds.addDataSourceProperty("url", getProperty("db.url", "jdbc:h2:mem:test;TRACE_LEVEL_FILE=4"));
    ds.addDataSourceProperty("user", "sa");
    ds.addDataSourceProperty("password", "");
    ds.setMaximumPoolSize(100);
    return ds;
  }

  @Bean
  public DatabaseInitializer dbInitializer(DataSource ds) {
    return new DatabaseInitializer(ds);
  }

}
