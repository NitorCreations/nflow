package com.nitorcreations.nflow.tests.runner;

import static java.lang.System.setProperty;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.server.Server;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.jetty.StartNflow;

public class NflowRunner extends SpringJUnit4ClassRunner {

  private static boolean initialized = false;
  private static String processName;
  private static Server nflowJetty;

  public NflowRunner(Class<?> klass) throws Exception {
    super(klass);
    synchronized (NflowRunner.class) {
      if (!initialized) {
        NflowRunner.startNflow();
        initialized = true;
      }
    }
  }

  private static void startNflow() throws Exception {
    processName = "nflow-tests-" + System.currentTimeMillis();
    setProperty("process.name", processName);
    nflowJetty = new StartNflow().startJetty(7500, "dev");
    for (int i = 0; i < 10000; i += 50) {
      if (nflowJetty.isStarted()) {
        break;
      }
      sleep(50);
    }
    assertTrue("Jetty did not start in 10 seconds", nflowJetty.isStarted());
  }

}
