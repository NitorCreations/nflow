package com.nitorcreations.nflow.tests.runner;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.right;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
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
  private Class<?> springContextClass;
  private JettyServerContainer nflowJetty;

  NflowServerRule(Builder b) {
    props = b.props;
    env = b.env;
    profiles = b.profiles;
    port = new AtomicReference<>(b.port);
    springContextClass = b.springContextClass;
  }

  public static class Builder {
    int port = 0;
    String env = "local";
    String profiles = "";
    Class<?> springContextClass;
    final Map<String, Object> props = new LinkedHashMap<>();
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

    public Builder springContextClass(Class<?> newSpringContextClass) {
      this.springContextClass = newSpringContextClass;
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
    return (String) props.get("nflow.executor.group");
  }

  public void stopServer() {
    stopJetty();
  }

  public void startServer() throws Exception {
    startJetty();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    if (!props.containsKey("nflow.executor.group")) {
      String processName = "nflow-tests-" + right(substringAfterLast(defaultString(description.getMethodName(), description.getClassName()), "."), 25) + "-" + currentTimeMillis();
      props.put("nflow.executor.group", processName);
    }
    return super.apply(base, description);
  }

  public void setSpringContextClass(Class<?> springContextClass) {
    this.springContextClass = springContextClass;
  }

  @Override
  protected void before() throws Throwable {
    startJetty();
  }

  @Override
  protected void after() {
    stopJetty();
  }

  private void startJetty() throws Exception {
    StartNflow startNflow = new StartNflow();
    if (springContextClass != null) {
      startNflow.registerSpringContext(springContextClass);
    }
    nflowJetty = startNflow.startJetty(port.get(), env, profiles, props);
    assertTrue("Jetty did not start", nflowJetty.isStarted());
    port.set(nflowJetty.getPort());
  }

  private void stopJetty() {
    try {
      nflowJetty.setStopTimeout(10000);
      nflowJetty.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertTrue("Jetty did not stop", nflowJetty.isStopped());
  }
}