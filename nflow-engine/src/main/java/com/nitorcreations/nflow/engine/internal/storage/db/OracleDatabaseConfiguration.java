package com.nitorcreations.nflow.engine.internal.storage.db;

import java.sql.Types;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

@Profile("nflow.db.oracle")
@Configuration
public class OracleDatabaseConfiguration extends DatabaseConfiguration {

  public OracleDatabaseConfiguration() {
    super("oracle");
  }

  @Bean
  public SQLVariants sqlVariants() {
    return new OracleSqlVariants();
  }

  public static class OracleSqlVariants implements SQLVariants {

    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + interval '" + seconds + "' second";
    }

    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    @Override
    public String nextActivationUpdate() {
      return "(case "
          + "when ? is null then null "
          + "when external_next_activation is null then ? "
          + "else least(?, external_next_activation) end)";
    }

    @Override
    public String workflowStatus(WorkflowInstanceStatus status) {
      return "'" + status.name() + "'";
    }

    @Override
    public String workflowStatus() {
      return "?";
    }

    @Override
    public String actionType() {
      return "?";
    }

    @Override
    public String castToText() {
      return "";
    }

    @Override
    public String limit(String query, String limit) {
      return "select * from (" + query + ") where rownum <= " + limit;
    }

    @Override
    public int longTextType() {
      return Types.CLOB;
    }
  }
}
