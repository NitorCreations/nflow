package io.nflow.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

import io.nflow.tests.runner.NflowServerRule;

public class SkipAutoStartTest extends AbstractNflowTest {

  // Because nflow.db.create_on_startup is false, no tables are created, which
  // causes failures if SQL statements are issued during bean initialization.
  // We need a valid database connection because connection to the database using
  // a database driver is done during bean initialization in the
  // DatabaseConfiguration class.
  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder()
      .prop("nflow.autostart", "false").prop("nflow.db.create_on_startup", "false")
    .build();

  public SkipAutoStartTest() {
    super(server);
  }

  @Test
  public void t01_startServerButNotNflow() {
    assertNotNull(server.getInstanceName());
  }

}
