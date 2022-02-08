package io.nflow.engine.service;

import io.nflow.engine.internal.dao.HealthCheckDao;

/**
 * Service for checking the database connection. The service can be e.g. hooked up to web service health check endpoint.
 */
public class HealthCheckService {

  private final HealthCheckDao healthCheckDao;

  public HealthCheckService(HealthCheckDao healthCheckDao) {
    this.healthCheckDao = healthCheckDao;
  }

  public boolean checkDatabaseConnection() {
    healthCheckDao.checkDatabaseConnection();
    return true;
  }
}
