package io.nflow.engine.exception;

import static org.joda.time.Duration.standardSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

public class DefaultStateSaveExceptionAnalyzerTest {

  final Environment env = new MockEnvironment().withProperty("nflow.executor.stateSaveRetryDelay.seconds", "10");
  final StateSaveExceptionAnalyzer analyzer = new DefaultStateSaveExceptionAnalyzer(env);

  @Test
  void analyzeReturnsConfiguredDelay() {
    StateSaveExceptionHandling handling = analyzer.analyze(new Exception());

    assertEquals(handling.logLevel, Level.ERROR);
    assertTrue(handling.logStackTrace);
    assertEquals(handling.retryDelay, standardSeconds(10));
  }
}
