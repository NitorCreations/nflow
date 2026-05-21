package io.nflow.tests.extension;

import java.util.List;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import org.slf4j.LoggerFactory;

public class ServerLogCaptureExtension implements BeforeAllCallback, TestExecutionExceptionHandler {

    @Override
    public void beforeAll(ExtensionContext context) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(ServerLogCaptureAppender.class.getName()) == null) {
            ServerLogCaptureAppender appender = new ServerLogCaptureAppender();
            appender.setName(ServerLogCaptureAppender.class.getName());
            appender.setContext(loggerContext);
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        List<String> logs = ServerLogCaptureAppender.getCapturedLogs(context.getRequiredTestClass().getSimpleName());
        if (logs.isEmpty()) {
            throw throwable;
        }
        AssertionError wrapper = new AssertionError(
                "Server-side WARN/ERROR during test:\n" + String.join("\n---\n", logs),
                throwable);
        wrapper.setStackTrace(throwable.getStackTrace());
        throw wrapper;
    }
}
