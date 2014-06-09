package com.nitorcreations.nflow.tests.runner;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.right;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.nitorcreations.nflow.jetty.StartNflow;

public class NflowServerRule extends ExternalResource {
  private Server nflowJetty;
  private int port = 7500;
  private String env = "local";
  private String profiles = "";
  private final Map<String, Object> props = new HashMap<>();
  {
    props.put("nflow.db.h2.tcp.port", "");
    props.put("nflow.db.h2.console.port", "");
  }

  public NflowServerRule port(int newPort) {
    this.port = newPort;
    return this;
  }

  public int getPort() {
    return port;
  }

  public NflowServerRule env(String newEnv) {
    this.env = newEnv;
    return this;
  }

  public NflowServerRule profiles(String newProfiles) {
    this.profiles = newProfiles;
    return this;
  }

  public NflowServerRule prop(String key, Object val) {
    props.put(key, val);
    return this;
  }

  public String getInstanceName() {
    return (String) props.get("nflow.instance.name");
  }

  @Override
  public Statement apply(Statement base, Description description) {
    if (!props.containsKey("nflow.instance.name")) {
      String processName = "nflow-tests-" + right(substringAfterLast(defaultString(description.getMethodName(), description.getClassName()), "."), 25) + "-" + currentTimeMillis();
      props.put("nflow.instance.name", processName);
    }
    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    nflowJetty = new StartNflow().startJetty(port, env, profiles, props);
    assertTrue("Jetty did not start", nflowJetty.isStarted());
  }

  @Override
  protected void after() {
    try {
      nflowJetty.setStopTimeout(100);
      nflowJetty.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
