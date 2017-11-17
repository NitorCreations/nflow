package io.nflow.engine.config.db;

import static io.nflow.engine.config.Profiles.H2;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Configuration for H2 database.
 */
@Profile(H2)
@Configuration
public class H2DatabaseConfiguration extends DatabaseConfiguration {

  /**
   * Create a new instance.
   */
  public H2DatabaseConfiguration() {
    super("h2");
  }

  /**
   * Creates a server for connecting to the in-memory database.
   * @param env The Spring environemnt for getting the configuration properties.
   * @return A TCP server.
   * @throws SQLException
   */
  @Bean(initMethod="start", destroyMethod="stop")
  Server h2TcpServer(Environment env) throws SQLException {
    String port = env.getProperty("nflow.db.h2.tcp.port");
    if (isBlank(port)) {
      return null;
    }
    return Server.createTcpServer("-tcp","-tcpAllowOthers","-tcpPort",port);
  }

  /**
   * Creates a console server for connecting to the in-memory database.
   * @param env The Spring environemnt for getting the configuration properties.
   * @return A TCP server.
   * @throws SQLException
   */
  @Bean(initMethod="start", destroyMethod="stop")
  Server h2ConsoleServer(Environment env) throws SQLException {
    String port = env.getProperty("nflow.db.h2.console.port");
    if (isBlank(port)) {
      return null;
    }
    return Server.createTcpServer("-webPort",port);
  }

  /**
   * Creates the SQL variants for H2 database.
   * @return SQL variants optimized for H2.
   */
  @Bean
  public SQLVariants sqlVariants() {
    return new H2SQLVariants();
  }

  /**
   * SQL variants optimized for H2.
   */
  public static class H2SQLVariants implements SQLVariants {

    /**
     * Returns SQL representing the current database time plus given amount of seconds.
     */
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "dateadd('second', " + seconds + ", current_timestamp)";
    }

    /**
     * Returns false as H2 does not support update returning clause.
     */
    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    /**
     * Returns false as H2 does not support updateable CTEs.
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
      return "(case "
          + "when ? is null then null "
          + "when external_next_activation is null then ? "
          + "else least(?, external_next_activation) end)";
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
     * Returns empty string as casting to text is not needed in H2.
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
     * Returns true as H2 suppports batch updates.
     */
    @Override
    public boolean useBatchUpdate() {
      return true;
    }
  }
}
