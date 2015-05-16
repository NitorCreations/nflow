package com.nitorcreations.nflow.jetty;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Configurable;

public class StartNflowTest {
  @Test
  public void startNflowJettyToRandomFreeLocalPort() throws Exception {
    JettyServerContainer jetty = initJettyStart(0, "jmx");
    assertThat(jetty.getPort(), is(not(0)));
    startStop(jetty);
  }

  private JettyServerContainer initJettyStart(String profiles) throws Exception {
    return initJettyStart(0, profiles);
  }

  private JettyServerContainer initJettyStart(int port, String profiles) throws Exception {
    return new StartNflow().registerSpringContext(DummyContext.class).startJetty(port, "junit", profiles);
  }

  private void startStop(JettyServerContainer jetty) throws Exception {
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

  @Ignore(value = "Not used generally to avoid port conflicts")
  @Test
  public void startNflowJettyToPredefinedPort() throws Exception {
    JettyServerContainer jetty = initJettyStart(7505, "");
    assertThat(jetty.getPort(), is(7505));
    startStop(jetty);
  }

  @Test
  @Ignore
  public void startNflowJettyMysql() throws Exception {
    startStop(initJettyStart("nflow.db.mysql"));
  }

  @Test
  @Ignore
  public void startNflowJettyPostgreSQL() throws Exception {
    startStop(initJettyStart("nflow.db.postgresql"));
  }

  @Configurable
  static class DummyContext {

  }
}
