package io.nflow.engine.internal.storage.db;

import java.sql.Types;

import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

public class MySQLVariants implements SQLVariants {
  @Override
  public String currentTimePlusSeconds(int seconds) {
    return "date_add(current_timestamp, interval " + seconds + " second)";
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
    return "(case " //
        + "when ? is null then null " //
        + "when external_next_activation is null then ? " //
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
    return query + " limit " + limit;
  }

  @Override
  public int longTextType() {
    return Types.VARCHAR;
  }

  @Override
  public boolean useBatchUpdate() {
    return true;
  }
}