package io.nflow.engine.internal.executor;

import static edu.umd.cs.mtc.TestFramework.runOnce;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import edu.umd.cs.mtc.MultithreadedTestCase;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.service.WorkflowDefinitionService;

@ExtendWith(MockitoExtension.class)
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
    when(executorDao.isTransactionSupportEnabled()).thenReturn(true);
    executor = new WorkflowInstanceExecutor(3, 2, 0, 10, 0, new CustomizableThreadFactory("nflow-executor-"));
    dispatcher = new WorkflowDispatcher(executor, workflowInstances, executorFactory, workflowDefinitions, executorDao, env);
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
    assertThrows(BeanCreationException.class, () -> new WorkflowDispatcher(executor, workflowInstances, executorFactory, workflowDefinitions, executorDao, env));
  }

  @Test
  public void exceptionDuringDispatcherExecutionCausesRetry() throws Throwable {
    // TODO MultithreadedTestCase depends on junit4
    // https://mvnrepository.com/artifact/edu.umd.cs.mtc/multithreadedtc, last updated 2009
    @SuppressWarnings("unused")
    class ExceptionDuringDispatcherExecutionCausesRetry extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenReturn(ids(1))
            .thenThrow(new RuntimeException("Expected: exception during dispatcher execution"))
            .thenAnswer(waitForTickAndAnswer(2, ids(2), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, noOpRunnable());
        when(executorFactory.createProcessor(1)).thenReturn(fakeWorkflowExecutor);
        WorkflowStateProcessor fakeWorkflowExecutor2 = fakeWorkflowExecutor(2, noOpRunnable());
        when(executorFactory.createProcessor(2)).thenReturn(fakeWorkflowExecutor2);
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
        inOrder.verify(executorFactory).createProcessor(1);
        inOrder.verify(executorFactory).createProcessor(2);
      }
    }
    runOnce(new ExceptionDuringDispatcherExecutionCausesRetry());
  }

  @Test
  public void errorDuringDispatcherExecutionStopsDispatcher() throws Throwable {
    @SuppressWarnings("unused")
    class ErrorDuringDispatcherExecutionStopsDispatcher extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenThrow(new AssertionError()).thenReturn(ids(1));
        try {
          dispatcher.run();
          Assertions.fail("Error should stop the dispatcher");
        } catch (AssertionError expected) {
          assertPoolIsShutdown(true);
        }
      }

      @Override
      public void finish() {
        verify(workflowInstances).pollNextWorkflowInstanceIds(anyInt());
        verify(executorFactory, never()).createProcessor(anyInt());
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
        verify(executorFactory, never()).createProcessor(anyInt());
      }
    }
    runOnce(new EmptyPollResultCausesNoTasksToBeScheduled());
  }

  @Test
  public void shutdownBlocksUntilPoolShutdown() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownBlocksUntilPoolShutdown extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(1), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyInt())).thenReturn(fakeWorkflowExecutor);
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
        assertPoolIsShutdown(true);
        verify(executorDao).markShutdown();
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
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            waitForTick(2);
            getThreadByName("threadShutdown").interrupt();
            return ids(1);
          }
        });
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyInt())).thenReturn(fakeWorkflowExecutor);
        dispatcher.run();
      }

      public void threadShutdown() {
        assertPoolIsShutdown(false);
        waitForTick(1);
        dispatcher.shutdown();
        waitForTick(3);
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
        dispatcher = new WorkflowDispatcher(poolSpy, workflowInstances, executorFactory, workflowDefinitions, executorDao, env);
      }

      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(), this));
        doThrow(new RuntimeException("Expected: exception on pool shutdown")).when(poolSpy).shutdown();
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(poolSpy).shutdown();
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

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
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
            .thenAnswer(waitForTickAndAnswer(2, Collections.<Integer> emptyList(), this));
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
        Assertions.fail("Expected warning was not logged");
      }
    }
    runOnce(new DispatcherLogsWarning());
  }

  @Test
  public void pauseAndResumeWorks() {
    assertEquals(dispatcher.isPaused(), false);
    dispatcher.pause();
    assertEquals(dispatcher.isPaused(), true);
    dispatcher.resume();
    assertEquals(dispatcher.isPaused(), false);
  }

  void assertPoolIsShutdown(boolean isTrue) {
    assertEquals(isTrue, executor.executor.isShutdown());
  }

  Runnable noOpRunnable() {
    return new Runnable() {
      @Override
      public void run() {
      }
    };
  }

  Runnable waitForTickRunnable(final int tick, final MultithreadedTestCase mtc) {
    return new Runnable() {
      @Override
      public void run() {
        mtc.waitForTick(tick);
      }
    };
  }

  WorkflowStateProcessor fakeWorkflowExecutor(int instanceId, final Runnable fakeCommand) {
    return new WorkflowStateProcessor(instanceId, null, null, null, null, null, env,
        new ConcurrentHashMap<Integer, WorkflowStateProcessor>(), (WorkflowExecutorListener) null) {
      @Override
      public void run() {
        fakeCommand.run();
      }
    };
  }

  Answer<List<Integer>> waitForTickAndAnswer(final int tick, final List<Integer> answer, final MultithreadedTestCase mtc) {
    return new Answer<List<Integer>>() {
      @Override
      public List<Integer> answer(InvocationOnMock invocation) {
        mtc.waitForTick(tick);
        return answer;
      }
    };
  }

  @SuppressWarnings("serial")
  static class ThreadPoolTaskExecutorWithThresholdQueue extends ThreadPoolTaskExecutor {
    @Override
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
      return new ThresholdBlockingQueue<>(queueCapacity, 0);
    }
  }

  static List<Integer> ids(Integer... ids) {
    return asList(ids);
  }
}
