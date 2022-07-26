package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.POSTGRESQL;

import java.sql.Types;

import io.nflow.engine.config.NFlowConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Configuration for PostgreSQL database.
 */
@Profile(POSTGRESQL)
@Configuration
public class PgDatabaseConfiguration extends DatabaseConfiguration {

  /**
   * Create a new instance.
   */
  public PgDatabaseConfiguration() {
    super("postgresql");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SQLVariants sqlVariants(NFlowConfiguration config) {
    return new PostgreSQLVariants();
  }

  /**
   * SQL variants optimized for PostgreSQL.
   */
  public static class PostgreSQLVariants implements SQLVariants {

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "current_timestamp + interval '" + seconds + " second'";
    }

    /**
     * Returns true as PostgreSQL supports update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return true;
    }

    /**
     * Returns true as PostgreSQL supports updateable CTEs.
     */
    @Override
    public boolean hasUpdateableCTE() {
      return true;
    }

    /**
     * Returns SQL representing the next activation time of the workflow instance.
     */
    @Override
    public String nextActivationUpdate() {
      return "(case "
          + "when ?::timestamptz is null then null "
          + "when external_next_activation is null then ?::timestamptz "
          + "else least(?::timestamptz, external_next_activation) end)";
    }

    /**
     * Returns the SQL representation for given workflow instance status.
     */
    @Override
    public String workflowStatus(WorkflowInstanceStatus status) {
      return "'" + status.name() + "'::workflow_status";
    }

    /**
     * Returns SQL representing the workflow instance status parameter.
     */
    @Override
    public String workflowStatus() {
      return "?::workflow_status";
    }

    /**
     * Returns SQL representing the action type parameter.
     */
    @Override
    public String actionType() {
      return "?::action_type";
    }

    /**
     * Returns string for casting value to text.
     */
    @Override
    public String castToText() {
      return "::text";
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
     * Returns true as PostgreSQL suppports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }

    /**
     * PostgreSQL suppports for update skip locked.
     */
    @Override
    public String forUpdateSkipLocked() {
      return " for update skip locked";
    }
  }
}
