package io.nflow.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

import io.nflow.tests.runner.NflowServerRule;

public class SkipAutoStartTest extends AbstractNflowTest {

  // When nflow.autoinit, nflow.autostart and nflow.db.create_on_startup are false
  // no database access should happen. This test fails if SQL statements are
  // issued during bean initialization.
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder()
      .prop("nflow.autoinit", "false").prop("nflow.autostart", "false").prop("nflow.db.create_on_startup", "false")
    .build();

  public SkipAutoStartTest() {
    super(server);
  }

  @Test
  public void t01_startServerButNotNflow() {
    assertNotNull(server.getInstanceName());
  }

}
