package com.nitorcreations.nflow.metrics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.health.HealthCheck;
import com.nitorcreations.nflow.engine.service.HealthCheckService;

public class DatabaseConnectionHealthCheckTest {
  HealthCheckService healthCheckService = mock(HealthCheckService.class);
  DatabaseConnectionHealthCheck check;

  @Before
  public void setup() {
     check = new DatabaseConnectionHealthCheck(healthCheckService);
  }

  @Test
  public void whenConnectingToDatabaseHealthCheckReturnsHealthly() {
    when(healthCheckService.checkDatabaseConnection()).thenReturn(true);
    assertEquals(true, check.check().isHealthy());
  }

  @Test
  public void whenNotConnectingToDatabaseHealthCheckReturnsUnhealthly() {
    RuntimeException exception = new RuntimeException("test-exception");
    when(healthCheckService.checkDatabaseConnection()).thenThrow(exception);

    HealthCheck.Result result = check.check();
    assertEquals(false, result.isHealthy());
    assertEquals(exception, result.getError());
  }
}
