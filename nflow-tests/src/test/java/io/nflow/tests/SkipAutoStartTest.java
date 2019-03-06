package io.nflow.tests;

import static org.junit.Assert.assertNotNull;

import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SkipAutoStartTest extends AbstractNflowTest {

  // When nflow.autoinit, nflow.autostart and nflow.db.create_on_startup are false
  // no database access should happen. This test fails if SQL statements are
  // issued during bean initialization.
  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.autoinit", "false").prop("nflow.autostart", "false").prop("nflow.db.create_on_startup", "false")
    .build();

  public SkipAutoStartTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void startServerButNotNflow() {
    assertNotNull(server.getInstanceName());
  }

}
