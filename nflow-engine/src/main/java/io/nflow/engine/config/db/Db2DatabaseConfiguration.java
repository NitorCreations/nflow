package io.nflow.engine.config.db;

import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.Types;

import static io.nflow.engine.config.Profiles.DB2;

/**
 * Configuration for SQL Server database.
 */
@Profile(DB2)
@Configuration
public class Db2DatabaseConfiguration extends DatabaseConfiguration {

  /**
   * Create a new instance.
   */
  public Db2DatabaseConfiguration() {
    super("db2");
  }

  /**
   * Creates the SQL variants for DB2 database.
   * @return SQL variants optimized for DB2.
   */
  @Bean
  public SQLVariants sqlVariants() {
    return new SQLServerVariants();
  }

  /**
   * SQL variants optimized for SQL Server.
   */
  public static class SQLServerVariants implements SQLVariants {

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + " + seconds + " SECONDS";
    }

    /**
     * Returns false as SQL Server does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as SQL Server does not support updateable CTEs.
     */
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
    public String dateLtEqDiff(String next_activation, String current_timestamp) {
      return "datediff_big(ms, " + next_activation + ", " + current_timestamp + ") >= 0";
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
     * Returns string for casting value to text.
     */
    @Override
    public String castToText() {
      return "";
    }

    /**
     * Returns SQL for a query with a limit of results.
     */
    @Override
    public String limit(String query, String limit) {
      int idx = query.indexOf("select ");
      return query.substring(0, idx + 7) + "top(" + limit + ") " + query.substring(idx + 7);
    }

    /**
     * Returns the SQL type for long text.
     */
    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }

    /**
     * Returns true as SQL Server suppports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }
  }
}
