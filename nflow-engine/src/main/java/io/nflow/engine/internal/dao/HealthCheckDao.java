package io.nflow.engine.internal.dao;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.inject.Named;

@Named
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
public class HealthCheckDao {
  private final JdbcTemplate jdbc;

  @Inject
  public HealthCheckDao(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "jdbc is injected")
  public void checkDatabaseConnection() {
    jdbc.query("select status, type from nflow_workflow where id = 0", resultSet -> null);
  }

}
