package com.nitorcreations.nflow.engine.internal.storage.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public int textType() {
      return Types.CLOB;
    }

    @Override
    public void setText(PreparedStatement ps, int parameterIndex, String value) throws SQLException {
      Clob clob = ps.getConnection().createClob();
      clob.setString(1, value);
      ps.setClob(parameterIndex, clob);
    }

    @Override
    public String getText(ResultSet rs, int columnIndex) throws SQLException {
      Clob clob = rs.getClob(columnIndex);
      try (Reader reader = clob.getCharacterStream()) {
        char[] arr = new char[1024];
        StringBuilder sb = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
          sb.append(arr, 0, numCharsRead);
        }
        return sb.toString();
      } catch (IOException e) {
        throw new SQLException("Failed to read CLOB from database", e);
      }
    }
  }
}
