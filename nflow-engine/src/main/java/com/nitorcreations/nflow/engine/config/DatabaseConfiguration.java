package com.nitorcreations.nflow.engine.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfiguration {

  @Bean(name="nflow-datasource")
  public DataSource datasource(Environment env) throws ClassNotFoundException {
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(env.getRequiredProperty("db.driver"));
    config.addDataSourceProperty("url", env.getRequiredProperty("db.url"));
    config.addDataSourceProperty("user", env.getRequiredProperty("db.user"));
    config.addDataSourceProperty("password", env.getRequiredProperty("db.password"));
    config.setMaximumPoolSize(env.getRequiredProperty("db.max.pool.size", Integer.class));
    return new HikariDataSource(config);
  }

  @Bean
  public DatabaseInitializer dbInitializer(Environment env) throws ClassNotFoundException {
    return new DatabaseInitializer(datasource(env), env);
  }

}
