package io.nflow.engine.internal.storage.db;

import java.sql.Types;

import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * SQL variants optimized for MySQL.
 */
public class MySQLVariants implements SQLVariants {

  /**
   * Returns SQL representing the current database time plus given amount of seconds.
   */
  @Override
  public String currentTimePlusSeconds(int seconds) {
    return "from_unixtime(unix_timestamp() + " + seconds + ")";
  }

  /**
   * Returns false as MySQL does not support updateable CTEs.
   */
  @Override
  public boolean hasUpdateableCTE() {
    return false;
  }

  /**
   * Returns SQL representing the next activation time of the workflow instance.
   */
  @Override
  public String nextActivationUpdate() {
    return "(case when ? is null then null when external_next_activation is null then ? else least(?, external_next_activation) end)";
  }

  /**
   * Returns the SQL representation for given workflow instance status.
   */
  @Override
  public String workflowStatus(WorkflowInstanceStatus status) {
    return "'" + status.name() + "'";
  }

  /**
   * Returns SQL representing the workflow instance status parameter.
   */
  @Override
  public String workflowStatus() {
    return "?";
  }

  /**
   * Returns SQL representing the action type parameter.
   */
  @Override
  public String actionType() {
    return "?";
  }

  /**
   * Returns empty string as casting to text is not needed in MySQL.
   */
  @Override
  public String castToText() {
    return "";
  }

  /**
   * Returns SQL for a query with a limit of results.
   */
  @Override
  public String limit(String query, long limit) {
    return query + " limit " + limit;
  }

  /**
   * Returns the SQL type for long text.
   */
  @Override
  public int longTextType() {
    return Types.VARCHAR;
  }

  /**
   * Returns true as MySQL supports batch updates.
   */
  @Override
  public boolean useBatchUpdate() {
    return true;
  }

  /**
   * Returns "like binary" for case-sensitive comparison.
   */
  @Override
  public String caseSensitiveLike() {
    return "like binary";
  }
}
