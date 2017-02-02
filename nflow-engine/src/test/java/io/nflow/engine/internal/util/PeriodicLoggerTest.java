package io.nflow.engine.internal.util;

import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class PeriodicLoggerTest {
    @Mock
    Logger logger;

    PeriodicLogger periodicLogger;
    Object[] params = new Object[]{new Object(), 1};
    final long now = 1443540008000L;

    @Before
    public void setup() {
        periodicLogger = new PeriodicLogger(logger, 60);
        setCurrentMillisFixed(now);
    }

    @After
    public void teardown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void periodicLoggerLogsAtFirstLogCall() {
        periodicLogger.info("foo {}", params);
        verify(logger, times(1)).info("foo {}", params);
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void periodicLoggerDoenstLogMoreThanOneTimeInPeriod() {
        periodicLogger.info("foo {}", params);
        periodicLogger.warn("foo {}", params);
        periodicLogger.info("bar {}", params);
        periodicLogger.warn("baz");
        setCurrentMillisFixed(now + 59 * 1000);
        verify(logger, times(1)).info("foo {}", params);
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void periodicLoggerLogsAgainWhenPeriodChanges() {
        periodicLogger.info("foo1 {}", params);
        verify(logger, times(1)).info("foo1 {}", params);
        setCurrentMillisFixed(now + 60 * 1000);
        periodicLogger.warn("foo2 {}", params);
        periodicLogger.warn("foo2 {}", params);
        verify(logger, times(1)).warn("foo2 {}", params);
        setCurrentMillisFixed(now + 110 * 1000);
        periodicLogger.info("foo3 {}", params);
        verifyNoMoreInteractions(logger);
    }
}
