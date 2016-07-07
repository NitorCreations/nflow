package io.nflow.metrics;

import com.codahale.metrics.health.HealthCheck;

import io.nflow.engine.service.HealthCheckService;

/**
 * Check that connection to nflow database can be made.
 */
public class DatabaseConnectionHealthCheck extends HealthCheck {

  private final HealthCheckService healthCheckService;

  public DatabaseConnectionHealthCheck(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @Override
  protected Result check() {
    try {
      healthCheckService.checkDatabaseConnection();
      return HealthCheck.Result.healthy("Connection to nFlow database is OK.");
    } catch(Exception e) {
      return HealthCheck.Result.unhealthy(e);
    }
  }
}
