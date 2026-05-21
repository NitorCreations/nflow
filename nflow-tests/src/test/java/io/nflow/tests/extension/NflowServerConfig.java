package io.nflow.tests.extension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.nflow.metrics.NflowMetricsContext;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import io.nflow.jetty.JettyServerContainer;
import io.nflow.jetty.StartNflow;

public class NflowServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(NflowServerConfig.class);
    // Workaround for H2 2.4 bug https://github.com/h2database/h2database/issues/4342:
    // run DDL on a direct (non-pooled) connection keyed by H2 URL, kept open for the whole test class.
    private static final Map<String, Connection> h2KeepaliveConnections = new ConcurrentHashMap<>();

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
        ServerLogCaptureAppender.setTestContext(testName);
        openH2KeepaliveConnectionIfNeeded();
        startJetty();
    }

    public void after() {
        stopJetty();
        closeH2KeepaliveConnectionIfNeeded();
    }

    public NflowServerConfig anotherServer(Map<String, Object> extraProps) {
        Builder b = new Builder();
        b.props.putAll(props);
        b.props.putAll(extraProps);
        return new NflowServerConfig(b.env(env).profiles(profiles).metrics(metrics).springContextClass(springContextClass));
    }

    private boolean isH2Profile() {
        return profiles.contains("nflow.db.h2") || !profiles.contains("nflow.db.");
    }

    private void openH2KeepaliveConnectionIfNeeded() {
        if (!isH2Profile() || !props.containsKey("nflow.db.h2.url")) {
            return;
        }
        String h2Url = props.get("nflow.db.h2.url").toString();
        h2KeepaliveConnections.computeIfAbsent(h2Url, url -> {
            try {
                Connection conn = DriverManager.getConnection(url, "sa", "");
                logger.info("Opened H2 keepalive connection to {}", url);
                // Run DDL on this connection, see https://github.com/h2database/h2database/issues/4342
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.setIgnoreFailedDrops(true);
                populator.setSqlScriptEncoding(UTF_8.name());
                populator.addScript(new ClassPathResource("scripts/db/h2.create.ddl.sql"));
                populator.populate(conn);
                return conn;
            } catch (Exception e) {
                logger.warn("Failed to open H2 keepalive connection or run DDL", e);
                return null;
            }
        });
    }

    private void closeH2KeepaliveConnectionIfNeeded() {
        if (!isH2Profile() || !props.containsKey("nflow.db.h2.url")) {
            return;
        }
        String h2Url = props.get("nflow.db.h2.url").toString();
        Connection conn = h2KeepaliveConnections.remove(h2Url);
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("Failed to close H2 keepalive connection", e);
            }
        }
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