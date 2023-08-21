package io.nflow.engine.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.HealthCheckDao;

/**
 * Service for checking the database connection. The service can be e.g. hooked up to web service health check endpoint.
 */
@Component
public class HealthCheckService {

  private final HealthCheckDao healthCheckDao;

  @Inject
  public HealthCheckService(HealthCheckDao healthCheckDao) {
    this.healthCheckDao = healthCheckDao;
  }

  public boolean checkDatabaseConnection() {
    healthCheckDao.checkDatabaseConnection();
    return true;
  }
}
