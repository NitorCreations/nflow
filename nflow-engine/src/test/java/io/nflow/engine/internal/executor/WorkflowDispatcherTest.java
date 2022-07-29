package io.nflow.engine.internal.executor;

import static java.lang.Boolean.FALSE;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.nflow.engine.exception.DispatcherExceptionAnalyzer;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.NflowLogger;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.service.WorkflowDefinitionService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class WorkflowDispatcherTest {
  WorkflowDispatcher dispatcher;
  WorkflowInstanceExecutor executor;
  MockEnvironment env = new MockEnvironment();
  @Mock
  WorkflowInstanceDao workflowInstances;
  @Mock
  WorkflowDefinitionService workflowDefinitions;
  @Mock
  ExecutorDao executorDao;
  @Mock
  WorkflowStateProcessorFactory executorFactory;
  @Mock
  Appender<ILoggingEvent> mockAppender;
  @Captor
  ArgumentCaptor<ILoggingEvent> loggingEventCaptor;
  final DispatcherExceptionAnalyzer exceptionAnalyzer = new DispatcherExceptionAnalyzer();
  final NflowLogger nflowLogger = new NflowLogger();

  @BeforeEach
  public void setup() {
    env.setProperty("nflow.autoinit", "true");
    env.setProperty("nflow.dispatcher.sleep.ms", "0");
    env.setProperty("nflow.dispatcher.executor.queue.wait_until_threshold", "0");
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    env.setProperty("nflow.executor.stuckThreadThreshold.seconds", "60");
    env.setProperty("nflow.executor.stateProcessingRetryDelay.seconds", "1");
    env.setProperty("nflow.executor.stateSaveRetryDelay.seconds", "60");
    env.setProperty("nflow.executor.stateVariableValueTooLongRetryDelay.minutes", "60");
    env.setProperty("nflow.db.workflowInstanceType.cacheSize", "10000");
    env.setProperty("nflow.autostart", "true");
    when(executorDao.isTransactionSupportEnabled()).thenReturn(true);
    when(executorDao.isAutoCommitEnabled()).thenReturn(true);
    when(executorDao.isAutoCommitEnabled()).thenReturn(true);
    when(executorDao.getMaxWaitUntil()).thenAnswer(mock -> now().plusSeconds(5));
    executor = new WorkflowInstanceExecutor(3, 2, 0, 10, 0, new CustomizableThreadFactory("nflow-executor-"));
    dispatcher = new WorkflowDispatcher(executor, workflowInstances, executorFactory, workflowDefinitions, executorDao,
        exceptionAnalyzer, nflowLogger, env);
    Logger logger = (Logger) getLogger(ROOT_LOGGER_NAME);
    logger.addAppender(mockAppender);
  }

  @AfterEach
  public void teardown() {
    Logger logger = (Logger) getLogger(ROOT_LOGGER_NAME);
    logger.detachAppender(mockAppender);
  }

  @Test
  public void workflowDispatcherCreationFailsWithoutTransactionSupport() {
    when(executorDao.isTransactionSupportEnabled()).thenReturn(false);
    assertThrows(BeanCreationException.class, () -> new WorkflowDispatcher(executor, workflowInstances, executorFactory,
        workflowDefinitions, executorDao, exceptionAnalyzer, nflowLogger, env));
  }

  @Test
  public void workflowDispatcherCreationFailsWithAutoCommitDisabled() {
    when(executorDao.isAutoCommitEnabled()).thenReturn(false);
    assertThrows(BeanCreationException.class, () -> new WorkflowDispatcher(executor, workflowInstances, executorFactory,
        workflowDefinitions, executorDao, exceptionAnalyzer, nflowLogger, env));
  }

  @Test
  public void exceptionDuringDispatcherExecutionCausesRetry() throws Throwable {
    @SuppressWarnings("unused")
    class ExceptionDuringDispatcherExecutionCausesRetry extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenReturn(ids(1L))
            .thenThrow(new RuntimeException("Expected: exception during dispatcher execution"))
            .thenAnswer(waitForTickAndAnswer(2, ids(2L), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, noOpRunnable());
        when(executorFactory.createProcessor(eq(1L), any())).thenReturn(fakeWorkflowExecutor);
        WorkflowStateProcessor fakeWorkflowExecutor2 = fakeWorkflowExecutor(2, noOpRunnable());
        when(executorFactory.createProcessor(eq(2L), any())).thenReturn(fakeWorkflowExecutor2);
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(workflowInstances, times(3)).pollNextWorkflowInstanceIds(anyInt());
        InOrder inOrder = inOrder(executorFactory);
        inOrder.verify(executorFactory).createProcessor(eq(1L), any());
        inOrder.verify(executorFactory).createProcessor(eq(2L), any());
      }
    }
    runOnce(new ExceptionDuringDispatcherExecutionCausesRetry());
  }

  @Test
  public void errorDuringDispatcherExecutionStopsDispatcher() throws Throwable {
    @SuppressWarnings("unused")
    class ErrorDuringDispatcherExecutionStopsDispatcher extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenThrow(new AssertionError()).thenReturn(ids(1L));
        try {
          dispatcher.run();
          fail("Error should stop the dispatcher");
        } catch (AssertionError expected) {
          assertPoolIsShutdown(true);
        }
      }

      @Override
      public void finish() {
        verify(workflowInstances).pollNextWorkflowInstanceIds(anyInt());
        verify(executorFactory, never()).createProcessor(anyLong(), any());
      }
    }
    runOnce(new ErrorDuringDispatcherExecutionStopsDispatcher());
  }

  @Test
  public void emptyPollResultCausesNoTasksToBeScheduled() throws Throwable {
    @SuppressWarnings("unused")
    class EmptyPollResultCausesNoTasksToBeScheduled extends MultithreadedTestCase {
      @SuppressWarnings("unchecked")
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenReturn(ids(), ids())
            .thenAnswer(waitForTickAndAnswer(2, ids(), this));
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(workflowInstances, times(3)).pollNextWorkflowInstanceIds(anyInt());
        verify(executorFactory, never()).createProcessor(anyLong(), any());
      }
    }
    runOnce(new EmptyPollResultCausesNoTasksToBeScheduled());
  }

  @Test
  public void shutdownBlocksUntilPoolShutdown() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownBlocksUntilPoolShutdown extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(1L), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyLong(), any())).thenReturn(fakeWorkflowExecutor);
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
        assertPoolIsShutdown(true);
        verify(executorDao).markShutdown(true);
      }
    }
    runOnce(new ShutdownBlocksUntilPoolShutdown());
  }

  @Test
  public void shutdownWithoutStart() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownWithoutStart extends MultithreadedTestCase {

      public void threadShutdown() {
        dispatcher.shutdown();
        assertThat(dispatcher.isRunning(), is(false));
      }
    }
    runOnce(new ShutdownWithoutStart());
  }

  @Test
  public void shutdownCanBeInterrupted() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownCanBeInterrupted extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(invocation -> {
          waitForTick(2);
          getThreadByName("threadShutdown").interrupt();
          return ids(1L);
        });
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyLong(), any())).thenReturn(fakeWorkflowExecutor);
        dispatcher.run();
      }

      public void threadShutdown() {
        assertPoolIsShutdown(false);
        waitForTick(1);
        dispatcher.shutdown();
        waitForTick(4);
        assertPoolIsShutdown(true);
      }
    }
    runOnce(new ShutdownCanBeInterrupted());
  }

  @Test
  public void exceptionOnPoolShutdownIsNotPropagated() throws Throwable {
    @SuppressWarnings("unused")
    class ExceptionOnPoolShutdownIsNotPropagated extends MultithreadedTestCase {
      private WorkflowInstanceExecutor poolSpy;

      @Override
      public void initialize() {
        poolSpy = Mockito.spy(executor);
        dispatcher = new WorkflowDispatcher(poolSpy, workflowInstances, executorFactory, workflowDefinitions, executorDao,
            exceptionAnalyzer, nflowLogger, env);
      }

      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(), this));
        doThrow(new RuntimeException("Expected: exception on pool shutdown")).when(poolSpy).shutdown(workflows -> assertThat(workflows, empty()), true);
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(poolSpy).shutdown(any(), anyBoolean());
      }
    }
    runOnce(new ExceptionOnPoolShutdownIsNotPropagated());
  }

  @Test
  public void shutdownCanBeCalledMultipleTimes() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownCanBeCalledMultipleTimes extends MultithreadedTestCase {
      public void threadDispatcher() throws InterruptedException {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(), this));
        dispatcher.run();
      }

      public void threadShutdown() throws InterruptedException {
        for (int i=0;; i++) {
          if (executor.queue.isEmpty()) {
            break;
          }
          assertThat(i, lessThan(10));
          sleep(50);
        }
        waitForTick(1);
        dispatcher.shutdown();
        waitForTick(2);
      }

      @Override
      public void finish() {
        assertPoolIsShutdown(true);
        dispatcher.shutdown();
      }
    }
    runOnce(new ShutdownCanBeCalledMultipleTimes());
  }

  @Test
  public void dispatcherLogsWarningWhenAllThreadsArePotentiallyStuck() throws Throwable {
    @SuppressWarnings("unused")
    class DispatcherLogsWarning extends MultithreadedTestCase {
      public void threadDispatcher() throws InterruptedException {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt()))
            .thenAnswer(waitForTickAndAnswer(2, emptyList(), this));
        when(executorFactory.getPotentiallyStuckProcessors()).thenReturn(executor.getThreadCount());
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(mockAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
        for (ILoggingEvent event : loggingEventCaptor.getAllValues()) {
          if (event.getLevel().equals(Level.WARN) && event.getFormattedMessage()
              .equals("2 of 2 state processor threads are potentially stuck (processing longer than 60 seconds)")) {
            return;
          }
        }
        fail("Expected warning was not logged");
      }
    }
    runOnce(new DispatcherLogsWarning());
  }

  @Test
  public void pauseAndResumeWorks() {
    assertThat(dispatcher.isPaused(), is(false));
    dispatcher.pause();
    assertThat(dispatcher.isPaused(), is(true));
    dispatcher.resume();
    assertThat(dispatcher.isPaused(), is(false));
  }

  void assertPoolIsShutdown(boolean isTrue) {
    assertThat(executor.executor.isShutdown(), is((isTrue)));
  }

  Runnable noOpRunnable() {
    return () -> {};
  }

  Runnable waitForTickRunnable(int tick, MultithreadedTestCase mtc) {
    return () -> mtc.waitForTick(tick);
  }

  WorkflowStateProcessor fakeWorkflowExecutor(long instanceId, Runnable fakeCommand) {
    return new WorkflowStateProcessor(instanceId, FALSE::booleanValue, null, null, null, null, null, null, env,
        new ConcurrentHashMap<>(), null, null, (WorkflowExecutorListener) null) {
      @Override
      public void run() {
        fakeCommand.run();
      }
    };
  }

  Answer<List<Long>> waitForTickAndAnswer(int tick, List<Long> answer, MultithreadedTestCase mtc) {
    return invocation -> {
      mtc.waitForTick(tick);
      return answer;
    };
  }

  // Converted from https://mvnrepository.com/artifact/edu.umd.cs.mtc/multithreadedtc (last updated 2009) to junit5
  static class MultithreadedTestCase {
    private static final boolean debug = false;
    final Map<String, Thread> threads = new HashMap<>();
    private final AtomicInteger tick = new AtomicInteger();
    final List<Throwable> errors = synchronizedList(new ArrayList<>());
    private final Map<Integer, List<Thread>> waiters = new ConcurrentHashMap<>();

    public Thread getThreadByName(String name) {
      return threads.get(name);
    }

    private void debug(String msg) {
      if (debug) {
        System.out.println(msg);
      }
    }

    public synchronized void waitForTick(int wantedTick) {
      Thread thread = currentThread();
      String name = thread.getName();
      debug(name + " waiting for tick " + wantedTick);
      waiters.computeIfAbsent(wantedTick, t -> synchronizedList(new ArrayList<>())).add(thread);
      for (int i=0; i<200; ++i) {
        if (tick.get() == wantedTick) {
          debug(name + " got tick " + wantedTick);
          waiters.get(wantedTick).remove(thread);
          return;
        }
        try {
          synchronized (thread) {
            debug(name + " still waiting for tick " + wantedTick);
            thread.wait(100);
            if (tick.get() == wantedTick) {
              debug(name + " got tick " + wantedTick);
              waiters.get(wantedTick).remove(thread);
              return;
            }
          }
        } catch (@SuppressWarnings("unused") InterruptedException ex) {
          throw new RuntimeException("Interrupted while waiting for tick");
        }
        boolean allWaiting = true;
        for (Thread t : threads.values()) {
          if (t == currentThread()) {
            continue;
          }
          if (t.getState() != Thread.State.WAITING && t.isAlive()) {
            if (Stream.of(t.getStackTrace()).map(StackTraceElement::getMethodName)
                    .limit(8)
                    .noneMatch(n -> n.contains("waitForTick") || n.equals("park"))) {
              debug(t.getName() + " not ready: " + Stream.of(t.getStackTrace()).map(StackTraceElement::getMethodName)
                      .collect(Collectors.joining(", ")));
              allWaiting = false;
              break;
            }
          }
        }
        if (allWaiting) {
          int previousTick = tick.get() - 1;
          if (previousTick > 0 && !waiters.getOrDefault(previousTick, emptyList()).isEmpty()) {
            debug("All waiting but tick is previous tick is still busy: " + waiters.get(previousTick));
            continue;
          }
          int nextTick = tick.incrementAndGet();
          List<Thread> wakeupThreads = waiters.getOrDefault(nextTick, emptyList());
          debug("notifying tick " + nextTick + ": " + wakeupThreads);
          wakeupThreads.forEach(t -> { synchronized (t) { t.notifyAll(); } });
        }
      }
      throw new RuntimeException("tick did not advance for thread " + name);
    }

    public void initialize() {
    }

    public void finish() {
    }

    public void start() {
      initialize();
      for (Thread t : threads.values()) {
        t.start();
      }
    }

    public void stop() {
      for (Thread t : threads.values()) {
        try {
          t.join(5_000);
        } catch (Throwable err) {
          errors.add(err);
        }
        if (t.isAlive()) {
          t.interrupt();
          try {
            t.join(5_000);
          } catch (Throwable err) {
            errors.add(err);
          }
          if (t.isAlive()) {
            errors.add(new AssertionError("Thread " + t.getName() + " did not die fast enough"));
          }
        }
      }
      finish();
    }
  }

  static void runOnce(MultithreadedTestCase test) throws Throwable {
    for (Method m : test.getClass().getMethods()) {
      String name = m.getName();
      if (name.startsWith("thread")) {
        test.threads.put(name, new Thread(name) {
          @Override
          public void run() {
            try {
              m.invoke(test);
            } catch (InvocationTargetException inv) {
              test.errors.add(inv.getCause());
            } catch (Throwable t) {
              test.errors.add(t);
            }
          }
        });
      }
    }
    try {
      test.start();
    } finally {
      try {
        test.stop();
      } catch (Throwable t) {
        test.errors.add(t);
      }
      if (!test.errors.isEmpty()) {
        if (test.errors.size() == 1) {
          throw test.errors.get(0);
        }
        assertAll(test.errors.stream().map(err -> () -> { throw err; }));
      }
    }
  }

  static List<Long> ids(Long... ids) {
    return asList(ids);
  }
}
