package io.nflow.engine.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import io.nflow.engine.internal.dao.PollingBatchException;
import io.nflow.engine.internal.dao.PollingRaceConditionException;

public class DispatcherExceptionAnalyzerTest {

  DispatcherExceptionAnalyzer analyzer = new DispatcherExceptionAnalyzer();

  @Test
  void analyzeGenericExceptionReturnsDefaults() {
    DispatcherExceptionHandling handling = analyzer.analyze(new Exception());

    assertTrue(handling.log);
    assertEquals(handling.logLevel, Level.ERROR);
    assertTrue(handling.logStackTrace);
    assertTrue(handling.sleep);
    assertFalse(handling.randomizeSleep);
  }

  @Test
  void analyzePollingRaceConditionException() {
    DispatcherExceptionHandling handling = analyzer.analyze(new PollingRaceConditionException("error"));

    assertTrue(handling.log);
    assertEquals(handling.logLevel, Level.DEBUG);
    assertFalse(handling.logStackTrace);
    assertTrue(handling.sleep);
    assertTrue(handling.randomizeSleep);
  }

  @Test
  void analyzePollingBatchException() {
    DispatcherExceptionHandling handling = analyzer.analyze(new PollingBatchException("error"));

    assertTrue(handling.log);
    assertEquals(handling.logLevel, Level.WARN);
    assertFalse(handling.logStackTrace);
    assertFalse(handling.sleep);
  }

  @Test
  void analyzeInterruptedException() {
    DispatcherExceptionHandling handling = analyzer.analyze(new InterruptedException());

    assertFalse(handling.log);
    assertFalse(handling.sleep);
  }
}
