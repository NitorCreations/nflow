package io.nflow.engine.service;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.internal.dao.HealthCheckDao;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckServiceTest {
  @Mock
  private HealthCheckDao dao;
  private HealthCheckService service;

  @Before
  public void setup() {
    service = new HealthCheckService(dao);
  }

  @Test
  public void checkDatabaseConnectionDelegatesToDao() {
    service.checkDatabaseConnection();
    verify(dao).checkDatabaseConnection();
  }

}
