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
  public DataSource datasource(Environment env) {
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(env.getRequiredProperty("nflow.db.driver"));
    config.addDataSourceProperty("url", env.getRequiredProperty("nflow.db.url"));
    config.addDataSourceProperty("user", env.getRequiredProperty("nflow.db.user"));
    config.addDataSourceProperty("password", env.getRequiredProperty("nflow.db.password"));
    config.setMaximumPoolSize(env.getRequiredProperty("nflow.db.max_pool_size", Integer.class));
    return new HikariDataSource(config);
  }

  @Bean
  public DatabaseInitializer dbInitializer(Environment env) {
    return new DatabaseInitializer(datasource(env), env);
  }

}
