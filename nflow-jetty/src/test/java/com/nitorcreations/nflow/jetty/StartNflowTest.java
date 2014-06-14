package com.nitorcreations.nflow.jetty;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jetty.server.Server;
import org.junit.Ignore;
import org.junit.Test;

public class StartNflowTest {

  @Test
  public void startNflowJetty() throws Exception {
    startStop("");
  }

  private void startStop(String profiles) throws Exception {
    Server jetty = new StartNflow().startJetty(7505, "junit", profiles);
    for (int i = 0; i < 5000; i+=50) {
      if (jetty.isStarted())
        break;
      sleep(50);
    }
    assertTrue("Jetty did not start in 5 seconds", jetty.isStarted());
    sleep(100);
    jetty.stop();
    for (int i = 0; i < 10000; i+=50) {
      if (jetty.isStopped())
        return;
      sleep(50);
    }
    fail("Jetty did not stop gracefully in 10 seconds");

  }

  @Test
  @Ignore
  public void startNflowJettyMysql() throws Exception {
    startStop("nflow.db.mysql");
  }

  @Test
  @Ignore
  public void startNflowJettyPostgreSQL() throws Exception {
    startStop("nflow.db.postgresql");
  }

}
