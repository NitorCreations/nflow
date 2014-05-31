package com.nitorcreations.nflow.engine.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("nflow.postgresql")
@Configuration
public class PgDatabaseConfiguration extends DatabaseConfiguration {
  public PgDatabaseConfiguration() {
    super("postgresql");
  }
}
