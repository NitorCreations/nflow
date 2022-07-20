package io.nflow.tests.extension;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.nflow.metrics.NflowMetricsContext;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.nflow.jetty.JettyServerContainer;
import io.nflow.jetty.StartNflow;

public class NflowServerConfig {
    private final Map<String, Object> props;
    private final String env;
    private final String profiles;
    private final AtomicReference<Integer> port;
    private Class<?> springContextClass;
    private JettyServerContainer nflowJetty;
    private boolean metrics;

    NflowServerConfig(Builder b) {
        props = b.props;
        env = b.env;
        profiles = b.profiles;
        port = new AtomicReference<>(b.port);
        springContextClass = b.springContextClass;
        metrics = b.metrics;
        if (b.clearProfiles) {
            props.put("clearProfiles", true);
        }
    }

    public static class Builder {
        int port = 0;
        String env = "local";
        String profiles = "";
        Class<?> springContextClass;
        boolean metrics = false;
        final Map<String, Object> props = new LinkedHashMap<>();
        boolean clearProfiles;

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

        public Builder metrics(boolean enableMetrics) {
            this.metrics = enableMetrics;
            return this;
        }

        public Builder clearProfiles() {
            this.clearProfiles = true;
            return this;
        }

        public NflowServerConfig build() {
            return new NflowServerConfig(this);
        }
    }

    public int getPort() {
        return port.get();
    }

    public String getInstanceName() {
        return (String) props.get("nflow.executor.group");
    }

    public String getHttpAddress() {
        return "http://localhost:" + getPort();
    }

    public void stopServer() {
        stopJetty();
    }

    public void startServer() throws Exception {
        startJetty();
    }

    public void setSpringContextClass(Class<?> springContextClass) {
        this.springContextClass = springContextClass;
    }

    public void before(String testName) throws Exception {
        if (getInstanceName() == null) {
            props.put("nflow.executor.group", testName);
        }
        startJetty();
    }

    public void after() {
        stopJetty();
    }

    public NflowServerConfig anotherServer(Map<String, Object> extraProps) {
        Builder b = new Builder();
        b.props.putAll(props);
        b.props.putAll(extraProps);
        return new NflowServerConfig(b.env(env).profiles(profiles).metrics(metrics).springContextClass(springContextClass));
    }

    private void startJetty() throws Exception {
        StartNflow startNflow = new StartNflow();
        if (springContextClass != null) {
            startNflow.registerSpringContext(springContextClass);
        }
        if (metrics) {
            startNflow.registerSpringContext(NflowMetricsContext.class);
        }
        nflowJetty = startNflow.startJetty(port.get(), env, profiles, props);
        assertTrue(nflowJetty.isStarted(), "Jetty did not start");
        port.set(nflowJetty.getPort());
    }

    private void stopJetty() {
        if (nflowJetty == null) {
            return;
        }
        try {
            nflowJetty.setStopTimeout(10000);
            nflowJetty.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(nflowJetty.isStopped(), "Jetty did not stop");
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}