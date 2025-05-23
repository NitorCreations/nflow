package io.nflow.jetty;

import static io.nflow.engine.config.Profiles.JMX;
import static io.nflow.engine.config.Profiles.MARIADB;
import static io.nflow.engine.config.Profiles.MYSQL;
import static io.nflow.engine.config.Profiles.POSTGRESQL;
import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StartNflowTest {

  @Test
  public void startNflowJettyToRandomFreeLocalPort() throws Exception {
    JettyServerContainer jetty = initJettyStart(JMX);
    assertThat(jetty.getPort(), is(not(0)));
    startStop(jetty);
  }

  @Test
  public void startNflowJettyToRandomFixedPort2() throws Exception {
    JettyServerContainer jetty = new StartNflow().registerSpringContext(DummyContext.class).startJetty(0, "junit", "ignore", new HashMap<>());
    assertThat(jetty.getPort(), is(not(0)));
    startStop(jetty);
  }

  @Test
  public void startNflowJettyToRandomFreeLocalPort3() throws Exception {
    JettyServerContainer jetty = new StartNflow().registerSpringContext(DummyContext.class).startJetty(Map.of("port", 0));
    assertThat(jetty.getPort(), is(not(0)));
    startStop(jetty);
  }

  private JettyServerContainer initJettyStart(String profiles) throws Exception {
    return new StartNflow().registerSpringContext(DummyContext.class).startJetty(0, "junit", profiles);
  }

  private void startStop(JettyServerContainer jetty) throws Exception {
    for (int i = 0; i < 5000; i+=50) {
      if (jetty.isStarted()) {
        break;
      }
      sleep(50);
    }
    assertTrue(jetty.isStarted(), "Jetty did not start in 5 seconds");
    sleep(100);
    jetty.stop();
    for (int i = 0; i < 10000; i+=50) {
      if (jetty.isStopped()) {
        return;
      }
      sleep(50);
    }
    fail("Jetty did not stop gracefully in 10 seconds");
  }

  @Configurable
  static class DummyContext {

  }
}
