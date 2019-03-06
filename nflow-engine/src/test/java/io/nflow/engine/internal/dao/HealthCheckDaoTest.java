package io.nflow.engine.internal.dao;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;

public class HealthCheckDaoTest extends BaseDaoTest {

  @Inject
  HealthCheckDao healthCheckDao;

  @Test
  public void checkDatabaseConnectionDoesntThrowExceptionWithWorkingDatabase() {
    healthCheckDao.checkDatabaseConnection();
  }
}
