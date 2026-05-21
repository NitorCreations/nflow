package io.nflow.tests.extension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled("Used only for testing utility tests - enable manually to verify ServerLogCaptureExtension")
@ExtendWith(ServerLogCaptureExtension.class)
public class ServerLogCaptureExtensionTest {

    private static final Logger logger = LoggerFactory.getLogger(ServerLogCaptureExtensionTest.class);

    @BeforeAll
    static void setupTestContext() {
        ServerLogCaptureAppender.setTestContext(ServerLogCaptureExtensionTest.class.getSimpleName());
    }

    @Test
    public void testWithWarnLog() {
        logger.warn("This is a test WARN message that should appear in test failure");
        Assertions.fail("This test is expected to fail - check if WARN log appears in error message");
    }
}
