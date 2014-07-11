package com.nitorcreations.nflow.tests.demo;

import com.nitorcreations.nflow.jetty.StartNflow;
import com.nitorcreations.nflow.metrics.NflowMetricsContext;

public class DemoServer {
  public static void main(String[] args) throws Exception {
    new StartNflow().registerSpringContext(NflowMetricsContext.class).startJetty(7500, "local", "jmx");
  }
}
