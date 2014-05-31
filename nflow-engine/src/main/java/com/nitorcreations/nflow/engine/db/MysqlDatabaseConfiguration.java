package com.nitorcreations.nflow.engine.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("nflow.mysql")
@Configuration
public class MysqlDatabaseConfiguration extends DatabaseConfiguration {
  public MysqlDatabaseConfiguration() {
    super("mysql");
  }
}
