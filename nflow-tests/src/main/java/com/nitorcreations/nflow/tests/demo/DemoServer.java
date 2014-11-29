package com.nitorcreations.nflow.tests.demo;

import com.nitorcreations.nflow.jetty.StartNflow;
import com.nitorcreations.nflow.metrics.NflowMetricsContext;

/**
 * To enable swagger ui (http://localhost:7500/docs) Execute "mvn compile"
 * before starting.
 */
public class DemoServer {
  public static void main(String[] args) throws Exception {
    new StartNflow().registerSpringContext(NflowMetricsContext.class).startJetty(7500, "local", "jmx");
  }
}
