package io.nflow.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.tests.extension.NflowServerConfig;

public class SkipAutoStartTest extends AbstractNflowTest {

  // When nflow.autoinit, nflow.autostart, nflow.db.create_on_startup and nflow.maintenance.insertWorkflowIfMissing are false, no
  // database access should happen. This test fails if SQL statements are issued during bean initialization.
  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .prop("nflow.autoinit", "false")
      .prop("nflow.autostart", "false")
      .prop("nflow.db.create_on_startup", "false")
      .prop("nflow.maintenance.insertWorkflowIfMissing", "false")
      .prop("nflow.db.initialization_fail_timeout_seconds", "-1")
      .prop("nflow.db.url", "jdbc:h2:/invalid/path/that/does/not/exist")
      .build();

  public SkipAutoStartTest() {
    super(server);
  }

  @Test
  public void startServerButNotNflow() {
    assertNotNull(server.getHttpAddress());
  }
}
