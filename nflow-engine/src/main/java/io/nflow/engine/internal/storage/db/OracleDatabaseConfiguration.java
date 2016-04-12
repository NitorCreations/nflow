package io.nflow.engine.internal.storage.db;

import static io.nflow.engine.internal.config.Profiles.ORACLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceUtils;

import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

@Profile(ORACLE)
@Configuration
public class OracleDatabaseConfiguration extends DatabaseConfiguration {

  private static final Logger logger = getLogger(OracleDatabaseConfiguration.class);
  public static final String DB_TYPE_ORACLE = "oracle";
  private boolean useBatchUpdate;

  public OracleDatabaseConfiguration() {
    super(DB_TYPE_ORACLE);
  }

  @Bean
  @Override
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("Oracle {}.{}, product version {}", majorVersion, minorVersion, meta.getDatabaseProductVersion());
      useBatchUpdate = (majorVersion > 12 || (majorVersion == 12 && minorVersion >= 1));
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain oracle version", e);
    }
    return new DatabaseInitializer(DB_TYPE_ORACLE, nflowDataSource, env);
  }

  @Bean
  @DependsOn(NFLOW_DATABASE_INITIALIZER)
  public SQLVariants sqlVariants() {
    return new OracleSqlVariants(useBatchUpdate);
  }

  public static class OracleSqlVariants implements SQLVariants {

    private final boolean useBatchUpdate;

    public OracleSqlVariants(boolean useBatchUpdate) {
      this.useBatchUpdate = useBatchUpdate;
    }

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
      return "select * from (" + query + ") where rownum <= " + limit;
    }

    @Override
    public int longTextType() {
      return Types.CLOB;
    }

    @Override
    public boolean useBatchUpdate() {
      return useBatchUpdate;
    }
  }
}
