package com.nitorcreations.nflow.engine.internal.storage.db;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("nflow.db.postgresql")
@Configuration
public class PgDatabaseConfiguration extends DatabaseConfiguration {
  public PgDatabaseConfiguration() {
    super("postgresql");
  }


  @Bean
  public SQLVariants sqlVariants() {
    return new PostgreSQLVariants();
  }

  public static class PostgreSQLVariants implements SQLVariants {
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + interval '" + seconds + " second'";
    }

    @Override
    public boolean hasUpdateReturning() {
      return true;
    }
  }
}
