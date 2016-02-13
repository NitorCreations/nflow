package com.nitorcreations.nflow.engine.service;

import com.nitorcreations.nflow.engine.internal.dao.HealthCheckDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

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
