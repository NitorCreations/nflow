package com.nitorcreations.nflow.metrics;

import com.codahale.metrics.health.HealthCheck;
import com.nitorcreations.nflow.engine.service.StatisticsService;

/**
 * Check that connection to nflow database can be made.
 */
public class DatabaseConnectionHealthCheck extends HealthCheck {

  private final StatisticsService statisticsService;

  public DatabaseConnectionHealthCheck(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @Override
  protected Result check() throws Exception {
    try {
      statisticsService.queryStatistics();
      return HealthCheck.Result.healthy("Connection to nFlow database is OK.");
    } catch(Exception e) {
      return HealthCheck.Result.unhealthy(e);
    }
  }
}
