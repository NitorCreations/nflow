package com.nitorcreations.nflow.tests.demo;

import com.nitorcreations.nflow.jetty.StartNflow;

public class DemoServer {
  public static void main(String[] args) throws Exception {
    new StartNflow().startJetty(7500, "local", "metrics");
  }
}
