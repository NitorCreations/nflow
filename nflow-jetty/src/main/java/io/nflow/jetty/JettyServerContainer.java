package io.nflow.jetty;

import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.ServerConnector;

// TODO: remove this wrapper?
public class JettyServerContainer {
  private final Server server;

  public JettyServerContainer(Server server) {
    this.server = server;
  }

  public boolean isStarted() {
    return server.isStarted();
  }

  public void stop() throws Exception {
    server.stop();
  }

  public boolean isStopped() {
    return server.isStopped();
  }

  public void setStopTimeout(int stopTimeout) {
    server.setStopTimeout(stopTimeout);
  }

  public int getPort() {
    return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
  }
}
