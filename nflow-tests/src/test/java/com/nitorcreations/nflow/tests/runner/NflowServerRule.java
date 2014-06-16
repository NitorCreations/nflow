package com.nitorcreations.nflow.tests.runner;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.right;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.nitorcreations.nflow.jetty.JettyServerContainer;
import com.nitorcreations.nflow.jetty.StartNflow;

public class NflowServerRule extends ExternalResource {
  private final Map<String, Object> props;
  private final String env;
  private final String profiles;
  private final AtomicReference<Integer> port;
  private JettyServerContainer nflowJetty;

  private NflowServerRule(Builder b) {
    props = b.props;
    env = b.env;
    profiles = b.profiles;
    port = new AtomicReference<>(b.port);
  }

  public static class Builder {
    private int port = 0;
    private String env = "local";
    private String profiles = "";
    private final Map<String, Object> props = new HashMap<>();
    {
      props.put("nflow.db.h2.tcp.port", "");
      props.put("nflow.db.h2.console.port", "");
    }

    public Builder port(int newPort) {
      this.port = newPort;
      return this;
    }

    public Builder env(String newEnv) {
      this.env = newEnv;
      return this;
    }

    public Builder profiles(String newProfiles) {
      this.profiles = newProfiles;
      return this;
    }

    public Builder prop(String key, Object val) {
      props.put(key, val);
      return this;
    }

    public NflowServerRule build() {
      return new NflowServerRule(this);
    }
  }

  public int getPort() {
    return port.get();
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
    nflowJetty = new StartNflow().startJetty(port.get(), env, profiles, props);
    assertTrue("Jetty did not start", nflowJetty.isStarted());
    port.set(nflowJetty.getPort());
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
