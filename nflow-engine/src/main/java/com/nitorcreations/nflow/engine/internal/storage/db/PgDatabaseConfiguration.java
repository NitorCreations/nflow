package com.nitorcreations.nflow.engine.internal.storage.db;

import static java.lang.String.format;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

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
    public String nextActivationUpdate() {
      return "(case "
          + "when ?::timestamptz is null then null "
          + "when external_next_activation is null then ?::timestamptz "
          + "else least(?::timestamptz, external_next_activation) end)";
    }

    @Override
    public String workflowStatus(WorkflowInstanceStatus status) {
      return "'" + status.name() + "'::workflow_status";
    }

    @Override
    public String workflowStatus() {
      return "?::workflow_status";
    }

    @Override
    public String actionType() {
      return "?::action_type";
    }

    @Override
    public String castToText() {
      return "::text";
    }
  }
}
