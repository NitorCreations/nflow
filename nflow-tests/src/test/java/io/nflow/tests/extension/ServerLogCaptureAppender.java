package io.nflow.tests.extension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

public class ServerLogCaptureAppender extends AppenderBase<ILoggingEvent> {
    // InheritableThreadLocal so Jetty worker threads (spawned during server startup) inherit the test context
    static final InheritableThreadLocal<String> testContext = new InheritableThreadLocal<>();
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> capturedLogsByTest = new ConcurrentHashMap<>();

    @Override
    protected void append(ILoggingEvent event) {
        if (!event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return;
        }
        String testName = testContext.get();
        if (testName == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(event.getLevel()).append(' ').append(event.getLoggerName())
          .append(" - ").append(event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            sb.append('\n').append(ThrowableProxyUtil.asString(event.getThrowableProxy()));
        }
        capturedLogsByTest.computeIfAbsent(testName, k -> new CopyOnWriteArrayList<>()).add(sb.toString());
    }

    public static void setTestContext(String testName) {
        testContext.set(testName);
        capturedLogsByTest.computeIfAbsent(testName, k -> new CopyOnWriteArrayList<>());
    }

    public static List<String> getCapturedLogs(String testName) {
        List<String> logs = capturedLogsByTest.remove(testName);
        return logs != null ? logs : Collections.emptyList();
    }
}
