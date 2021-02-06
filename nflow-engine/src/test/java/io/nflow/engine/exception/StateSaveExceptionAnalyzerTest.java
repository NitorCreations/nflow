package io.nflow.engine.exception;

import static org.joda.time.Duration.standardSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

public class StateSaveExceptionAnalyzerTest {

  final Environment env = new MockEnvironment().withProperty("nflow.executor.stateSaveRetryDelay.seconds", "10");

  @Test
  void defaultAnalyzerReturnsConfiguredDelay() {
    StateSaveExceptionAnalyzer analyzer = new StateSaveExceptionAnalyzer(env);

    StateSaveExceptionHandling handling = analyzer.analyzeSafely(new Exception(), 0);

    assertEquals(handling.logLevel, Level.ERROR);
    assertTrue(handling.logStackTrace);
    assertEquals(handling.retryDelay, standardSeconds(10));
  }

  @Test
  void customerAnalyzerCanBeUsed() {
    StateSaveExceptionAnalyzer analyzer = new StateSaveExceptionAnalyzer(env) {
      @Override
      protected StateSaveExceptionHandling analyze(Exception e, int saveRetryCount) {
        return new StateSaveExceptionHandling.Builder().setRetryDelay(standardSeconds(1)).build();
      }
    };

    StateSaveExceptionHandling handling = analyzer.analyzeSafely(new Exception(), 0);

    assertEquals(handling.logLevel, Level.ERROR);
    assertTrue(handling.logStackTrace);
    assertEquals(handling.retryDelay, standardSeconds(1));
  }

  @Test
  void defaultAnalyzerIsUsedWhenCustomerAnalyzerFails() {
    StateSaveExceptionAnalyzer analyzer = new StateSaveExceptionAnalyzer(env) {
      @Override
      protected StateSaveExceptionHandling analyze(Exception e, int saveRetryCount) {
        throw new IllegalStateException("fail");
      }
    };

    StateSaveExceptionHandling handling = analyzer.analyzeSafely(new Exception(), 0);

    assertEquals(handling.logLevel, Level.ERROR);
    assertTrue(handling.logStackTrace);
    assertEquals(handling.retryDelay, standardSeconds(10));
  }
}
