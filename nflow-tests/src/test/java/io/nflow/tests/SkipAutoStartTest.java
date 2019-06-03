package io.nflow.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SkipAutoStartTest extends AbstractNflowTest {

  /**
   * When nflow.definition.autopersist, nflow.autostart and nflow.db.create_on_startup are false no database access should happen.
   * This test fails if SQL statements are issued during bean initialization.
   */
  public static NflowServerConfig server = new NflowServerConfig.Builder() //
      .prop("nflow.definition.autopersist", "false") //
      .prop("nflow.autostart", "false") //
      .prop("nflow.db.create_on_startup", "false") //
      .build();

  public SkipAutoStartTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void startServerButNotNflow() {
    assertNotNull(server.getHttpAddress());
  }

}
