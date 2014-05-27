package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.nitorcreations.nflow.engine.service.RepositoryService;

public class WorkflowDispatcherTest extends BaseNflowTest {
  private WorkflowDispatcher dispatcher;

  private ThreadPoolTaskExecutor pool = dispatcherPoolExecutor() ;

  @Mock
  private RepositoryService repository;

  @Mock
  private WorkflowExecutorFactory executorFactory;

  @Mock
  private Environment env;

  @Before
  public void setup() {
    when(env.getProperty("dispatcher.sleep.ms", Long.class, 5000l)).thenReturn(0l);

    dispatcher = new WorkflowDispatcher(pool, repository, executorFactory, env);
  }

  @Test
  public void exceptionDuringDispatcherExecutionCausesRetry() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt()))
        .thenReturn(asList(1))
        .thenThrow(new RuntimeException("Expected: exception during dispatcher execution"))
        .thenAnswer(new ShutdownRequestDispatcher() {
          @Override
          protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
            return asList(2);
          }
        });

    when(executorFactory.createExecutor(1)).thenReturn(fakeWorkflowExecutor(1, noOpRunnable()));
    when(executorFactory.createExecutor(2)).thenReturn(fakeWorkflowExecutor(2, noOpRunnable()));

    dispatcher.run();
    assertPoolIsShutdown(true);

    verify(repository, times(3)).pollNextWorkflowInstanceIds(anyInt());
    InOrder inOrder = inOrder(executorFactory);
    inOrder.verify(executorFactory).createExecutor(1);
    inOrder.verify(executorFactory).createExecutor(2);
  }

  @Test
  public void errorDuringDispatcherExecutionStopsDispatcher() {
    when(repository.pollNextWorkflowInstanceIds(anyInt()))
        .thenThrow(new AssertionError())
        .thenReturn(asList(1));

    try {
      dispatcher.run();
      Assert.fail("Error should stop the dispatcher");
    } catch (AssertionError expected) {
      assertPoolIsShutdown(true);
    }

    verify(repository).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory, never()).createExecutor(anyInt());
  }

  @Test
  public void emptyPollResultCausesNoTasksToBeScheduled() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt()))
        .thenReturn(new ArrayList<Integer>(), new ArrayList<Integer>())
        .thenAnswer(new ShutdownRequestDispatcher() {
          @Override
          protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
            return new ArrayList<Integer>();
          }
        });

    dispatcher.run();

    verify(repository, times(3)).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory, never()).createExecutor(anyInt());
  }

  @Test
  public void shutdownBlocksUntilPoolShutdown() throws InterruptedException {
    ShutdownRequestDispatcher shutdownRequestDispatcher = new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        return asList(1);
      }
    };
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(shutdownRequestDispatcher);
    when(executorFactory.createExecutor(anyInt())).thenAnswer(
        workflowExecutorAnswer(delayedRunnable(Sandman.SHORT_DELAY_MS)));

    Thread t = new Thread(dispatcher);
    t.start();
    shutdownRequestDispatcher.waitUntilFinished();

    try {
      assertPoolIsShutdown(true);
    } finally {
      t.join();
    }

    verify(repository).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory, times(1)).createExecutor(anyInt());
  }

  @Test
  public void shutdownCanBeInterrupted() throws InterruptedException {
    ShutdownRequestDispatcher shutdownRequestDispatcher = new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        interruptShutdownThread();
        return asList(1);
      }
    };
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(shutdownRequestDispatcher);
    when(executorFactory.createExecutor(anyInt())).thenAnswer(
        workflowExecutorAnswer(delayedRunnable(Sandman.SHORT_DELAY_MS)));

    Thread t = new Thread(dispatcher);
    t.start();
    shutdownRequestDispatcher.waitUntilFinished();

    try {
      assertPoolIsShutdown(false);
    } finally {
      t.join();
    }
  }

  @Test
  public void exceptionOnPoolShutdownIsNotPropagated() throws InterruptedException {
    ThreadPoolTaskExecutor poolSpy = Mockito.spy(pool);
    dispatcher = new WorkflowDispatcher(poolSpy, repository, executorFactory, env);

    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        return new ArrayList<Integer>();
      }
    });
    doThrow(new RuntimeException("Expected: exception on pool shutdown")).when(poolSpy).shutdown();

    dispatcher.run();
  }

  @Test
  public void shutdownCanBeCalledMultipleTimes() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        return new ArrayList<Integer>();
      }
    });

    dispatcher.run();
    assertPoolIsShutdown(true);

    Thread t = newShutdownThread();
    t.start();
    t.join();
  }

  private abstract class ShutdownRequestDispatcher implements Answer<List<Integer>> {
    private Thread shutdownThread;
    private CountDownLatch shutdownThreadStarted = new CountDownLatch(1);

    public ShutdownRequestDispatcher() {
      this(new Runnable() {
        @Override
        public void run() {
          dispatcher.shutdown();
        }
      });
    }

    public ShutdownRequestDispatcher(Runnable shutdownCommand) {
      shutdownThread = new Thread(shutdownCommand);
    }

    @Override
    public final List<Integer> answer(InvocationOnMock invocation) throws Throwable {
      shutdownThread.start();
      shutdownThreadStarted.countDown();
      Sandman.sleep(Sandman.SHORT_DELAY_MS);
      return afterShutdownRequestDispatch(invocation);
    }

    public void interruptShutdownThread() {
      shutdownThread.interrupt();
    }

    public void waitUntilFinished() throws InterruptedException {
      shutdownThreadStarted.await();
      shutdownThread.join();
    }

    protected abstract List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation);
  }

  private Thread newShutdownThread() {
    return new Thread(new Runnable() {
      @Override
      public void run() {
        dispatcher.shutdown();
      }
    });
  }

  private static  ThreadPoolTaskExecutor dispatcherPoolExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    Integer threadCount = 2 * Runtime.getRuntime().availableProcessors();
    executor.setCorePoolSize(threadCount);
    executor.setMaxPoolSize(threadCount);
    executor.setKeepAliveSeconds(0);
    executor.setAwaitTerminationSeconds(60);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadFactory(new CustomizableThreadFactory("nflow-executor-"));
    executor.afterPropertiesSet();
    return executor;
  }

  private void assertPoolIsShutdown(boolean isTrue) {
    assertEquals(pool.getThreadPoolExecutor().isShutdown(), isTrue);
  }

  private Runnable noOpRunnable() {
    return new Runnable() {
      @Override
      public void run() {
      }
    };
  }

  private Runnable delayedRunnable(final long delayMs) {
    return new Runnable() {
      @Override
      public void run() {
        Sandman.sleep(delayMs);
      }
    };
  }

  private WorkflowExecutor fakeWorkflowExecutor(int instanceId, final Runnable fakeCommand) {
    return new WorkflowExecutor(instanceId, null) {
      @Override
      public void run() {
        fakeCommand.run();
      }
    };
  }

  private Answer<WorkflowExecutor> workflowExecutorAnswer(final Runnable fakeCommand) {
    return new Answer<WorkflowExecutor>() {
      @Override
      public WorkflowExecutor answer(InvocationOnMock invocation) throws Throwable {
        Integer instanceId = (Integer) invocation.getArguments()[0];
        return fakeWorkflowExecutor(instanceId, fakeCommand);
      }
    };
  }
}
