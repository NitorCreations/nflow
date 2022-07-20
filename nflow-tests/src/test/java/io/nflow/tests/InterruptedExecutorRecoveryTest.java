package io.nflow.tests;

import io.nflow.tests.extension.NflowServerConfig;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import static java.util.Collections.singletonMap;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InterruptedExecutorRecoveryTest extends AbstractExecutorRecoveryTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.executor.timeout.seconds", 1)
      .prop("nflow.executor.keepalive.seconds", 5)
      .prop("nflow.dispatcher.await.termination.seconds", 1)
      .prop("nflow.db.h2.url", "jdbc:h2:mem:interruptedexecutorrecoverytest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
      .build();

  public InterruptedExecutorRecoveryTest() {
    super(server, singletonMap("ignoreThreadInterrupt", false), 0, 1);
  }

}
