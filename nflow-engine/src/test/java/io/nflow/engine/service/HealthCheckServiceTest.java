package io.nflow.engine.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import io.nflow.engine.internal.dao.HealthCheckDao;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HealthCheckServiceTest {
  @Mock
  private HealthCheckDao dao;
  private HealthCheckService service;

  @BeforeEach
  public void setup() {
    service = new HealthCheckService(dao);
  }

  @Test
  public void checkDatabaseConnectionDelegatesToDao() {
    service.checkDatabaseConnection();
    verify(dao).checkDatabaseConnection();
  }

}
