package io.nflow.engine.internal.dao;

import javax.inject.Inject;

import org.junit.Test;

public class HealthCheckDaoTest extends BaseDaoTest {

  @Inject
  HealthCheckDao healthCheckDao;

  @Test
  public void checkDatabaseConnectionDoesntThrowExceptionWithWorkingDatabase() {
    healthCheckDao.checkDatabaseConnection();
  }
}
