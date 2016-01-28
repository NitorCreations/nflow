package com.nitorcreations.nflow.metrics;

import com.codahale.metrics.health.HealthCheck;
import com.nitorcreations.nflow.engine.service.StatisticsService;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseConnectionHealthCheckTest {
  StatisticsService statisticsService = mock(StatisticsService.class);
  DatabaseConnectionHealthCheck check;

  @Before
  public void setup() {
     check = new DatabaseConnectionHealthCheck(statisticsService);
  }

  @Test
  public void whenConnectingToDatabaseHealthCheckReturnsHealthly() throws Exception {
    when(statisticsService.queryStatistics()).thenReturn(mock(Statistics.class));
    assertEquals(true, check.check().isHealthy());
  }

  @Test
  public void whenNotConnectingToDatabaseHealthCheckReturnsUnhealthly() throws Exception {
    RuntimeException exception = new RuntimeException("test-exception");
    when(statisticsService.queryStatistics()).thenThrow(exception);

    HealthCheck.Result result = check.check();
    assertEquals(false, result.isHealthy());
    assertEquals(exception, result.getError());
  }
}
