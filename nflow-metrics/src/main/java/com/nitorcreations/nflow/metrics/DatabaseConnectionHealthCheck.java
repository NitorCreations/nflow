package com.nitorcreations.nflow.metrics;

import com.codahale.metrics.health.HealthCheck;
import com.nitorcreations.nflow.engine.service.HealthCheckService;
import com.nitorcreations.nflow.engine.service.StatisticsService;

/**
 * Check that connection to nflow database can be made.
 */
public class DatabaseConnectionHealthCheck extends HealthCheck {

  private final HealthCheckService healthCheckService;

  public DatabaseConnectionHealthCheck(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @Override
  protected Result check() throws Exception {
    try {
      healthCheckService.checkDatabaseConnection();
      return HealthCheck.Result.healthy("Connection to nFlow database is OK.");
    } catch(Exception e) {
      return HealthCheck.Result.unhealthy(e);
    }
  }
}
