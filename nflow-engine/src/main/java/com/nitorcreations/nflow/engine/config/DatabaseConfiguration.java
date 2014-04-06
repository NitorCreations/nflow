package com.nitorcreations.nflow.engine.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public DataSource datasource(Environment env) throws ClassNotFoundException {
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(env.getRequiredProperty("db.driver"));
    config.addDataSourceProperty("url", env.getRequiredProperty("db.url"));
    config.addDataSourceProperty("user", env.getRequiredProperty("db.user"));
    config.addDataSourceProperty("password", env.getRequiredProperty("db.password"));
    config.setMaximumPoolSize(100);
    return new HikariDataSource(config);
  }

  @Bean
  public DatabaseInitializer dbInitializer(DataSource ds, Environment env) {
    return new DatabaseInitializer(ds, env);
  }

}
