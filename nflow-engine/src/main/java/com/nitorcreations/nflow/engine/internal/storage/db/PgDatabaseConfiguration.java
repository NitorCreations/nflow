package com.nitorcreations.nflow.engine.internal.storage.db;

import java.sql.Types;

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

    @Override
    public String limit(String query, String limit) {
      return query + " limit " + limit;
    }

    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }
  }
}
