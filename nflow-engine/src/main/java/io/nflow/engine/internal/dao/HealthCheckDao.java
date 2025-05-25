package io.nflow.engine.internal.dao;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;

import io.nflow.engine.config.NFlow;

@Named
public class HealthCheckDao {
  private final JdbcTemplate jdbc;

  @Inject
  public HealthCheckDao(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  public void checkDatabaseConnection() {
    jdbc.query("select status, type from nflow_workflow where id = 0", resultSet -> null);
  }

}
