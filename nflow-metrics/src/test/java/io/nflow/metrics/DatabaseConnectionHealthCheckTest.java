package io.nflow.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;

import io.nflow.engine.service.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseConnectionHealthCheckTest {
  HealthCheckService healthCheckService = mock(HealthCheckService.class);
  DatabaseConnectionHealthCheck check;

  @BeforeEach
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
