package com.nitorcreations.nflow.tests.demo;

import com.nitorcreations.nflow.jetty.StartNflow;

public class DemoServer {

  public static void main(String[] args) throws Exception {
    new StartNflow().startTcpServerForH2().startJetty(7500, "dev");
  }
}
