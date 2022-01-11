package io.nflow.tests.extension;

import static io.nflow.engine.config.Profiles.POSTGRESQL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.V11_1;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.nflow.jetty.JettyServerContainer;
import io.nflow.jetty.StartNflow;
import ru.yandex.qatools.embed.postgresql.PostgresExecutable;
import ru.yandex.qatools.embed.postgresql.PostgresProcess;
import ru.yandex.qatools.embed.postgresql.PostgresStarter;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

public class NflowServerConfig {
    private final Map<String, Object> props;
    private final String env;
    private final String profiles;
    private final AtomicReference<Integer> port;
    private Class<?> springContextClass;
    private JettyServerContainer nflowJetty;
    private PostgresProcess process;

    NflowServerConfig(Builder b) {
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
        startDb();
        startJetty();
    }

    public void after() {
        stopJetty();
        stopDb();
    }

    public NflowServerConfig anotherServer() {
        Builder b = new Builder();
        b.props.putAll(props);
        return new NflowServerConfig(b.env(env).profiles(profiles).springContextClass(springContextClass));
    }

    private void startDb() throws IOException {
        if (profiles.contains(POSTGRESQL) && !props.containsKey("nflow.db.postgresql.url")) {
            PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getDefaultInstance();
            PostgresConfig config = new PostgresConfig(V11_1, new AbstractPostgresConfig.Net(),
                    new AbstractPostgresConfig.Storage("nflow"), new AbstractPostgresConfig.Timeout(),
                    new AbstractPostgresConfig.Credentials("nflow", "nflow"));
            PostgresExecutable exec = runtime.prepare(config);
            process = exec.start();
            props.put("nflow.db.postgresql.url", "jdbc:postgresql://" + config.net().host() + ":" + config.net().port() + "/nflow");
        }
    }

    private void stopDb() {
        if (process != null) {
            process.stop();
        }
    }

    private void startJetty() throws Exception {
        StartNflow startNflow = new StartNflow();
        if (springContextClass != null) {
            startNflow.registerSpringContext(springContextClass);
        }
        nflowJetty = startNflow.startJetty(port.get(), env, profiles, props);
        assertTrue(nflowJetty.isStarted(), "Jetty did not start");
        port.set(nflowJetty.getPort());
    }

    private void stopJetty() {
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