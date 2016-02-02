package com.nitorcreations.nflow.engine.service;

import com.nitorcreations.nflow.engine.internal.dao.HealthCheckDao;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
