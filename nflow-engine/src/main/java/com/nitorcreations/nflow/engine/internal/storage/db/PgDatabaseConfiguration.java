package com.nitorcreations.nflow.engine.internal.storage.db;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static java.lang.String.format;

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

    @Override
    public String castToEnumType(String variable, String type) {
      return variable + "::" + type;
    }

    @Override
    public boolean hasUpdateableCTE() {
      return true;
    }

    @Override
    public String least(String value1, String value2) {
      return format("(case " +
                      "when %1$s is null then %2$s " +
                      "when %2$s is null then %1$s " +
                      "when %1$s < %2$s then %1$s " +
                      "else %2$s end)",
              value1, value2);
    }

    @Override
    public String nextActivationUpdate(String value1, String value2) {
      return format("(case " +
                      "when %1$s::timestamptz is null then null " +
                      "when %2$s is null then %1$s::timestamptz " +
                      "when %1$s::timestamptz < %2$s then %1$s::timestamptz " +
                      "else %2$s end)",
              value1, value2);
    }
  }
}
