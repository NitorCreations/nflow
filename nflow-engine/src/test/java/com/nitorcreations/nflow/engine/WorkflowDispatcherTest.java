package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.nitorcreations.nflow.engine.service.RepositoryService;

public class WorkflowDispatcherTest extends BaseNflowTest {
  private WorkflowDispatcher dispatcher;
  private Thread shutdownThread;

  @Mock
  private ThreadPoolTaskExecutor pool;

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
    shutdownThread = newShutdownThread();
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

    dispatcher.run();
    shutdownThread.join();

    verify(repository, times(3)).pollNextWorkflowInstanceIds(anyInt());
    InOrder inOrder = inOrder(executorFactory);
    inOrder.verify(executorFactory).createExecutor(1);
    inOrder.verify(executorFactory).createExecutor(2);
    verify(pool, times(2)).execute(Mockito.any(Runnable.class));
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
    }

    verify(repository).pollNextWorkflowInstanceIds(anyInt());
    verify(pool, never()).execute(any(Runnable.class));
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
    shutdownThread.join();

    verify(repository, times(3)).pollNextWorkflowInstanceIds(anyInt());
    verify(pool, never()).execute(Mockito.any(Runnable.class));
  }

  @Test
  public void shutdownBlocksUntilPoolShutdown() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        return ints(100);
      }
    });
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        assertTrue(shutdownThread.isAlive());
        return null;
      }
    }).when(pool).shutdown();

    dispatcher.run();
    shutdownThread.join();

    verify(repository).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory, times(100)).createExecutor(anyInt());
    InOrder inOrder = inOrder(pool);
    inOrder.verify(pool, times(100)).execute(Mockito.any(Runnable.class));
    inOrder.verify(pool).shutdown();
  }

  @Test
  public void shutdownCanBeInterrupted() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        shutdownThread.interrupt();
        return ints(100);
      }
    });
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        assertFalse(shutdownThread.isAlive());
        return null;
      }
    }).when(pool).shutdown();

    dispatcher.run();
    shutdownThread.join();

    verify(pool).shutdown();
  }

  @Test
  public void exceptionOnPoolShutdownIsNotPropagated() throws InterruptedException {
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new ShutdownRequestDispatcher() {
      @Override
      protected List<Integer> afterShutdownRequestDispatch(InvocationOnMock invocation) {
        return new ArrayList<Integer>();
      }
    });
    doThrow(new RuntimeException("Expected: exception on pool shutdown")).when(pool).shutdown();

    dispatcher.run();
    shutdownThread.join();

    verify(pool).shutdown();
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
    Thread t = newShutdownThread();
    t.start();
    shutdownThread.join();
    t.join();
  }

  private abstract class ShutdownRequestDispatcher implements Answer<List<Integer>> {
    @Override
    public final List<Integer> answer(InvocationOnMock invocation) throws Throwable {
      shutdownThread.start();
      Sandman.sleep(Sandman.SHORT_DELAY_MS);
      return afterShutdownRequestDispatch(invocation);
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

  private List<Integer> ints(int howMany) {
    List<Integer> ints = new ArrayList<Integer>();
    for (int i = 0; i < howMany; i++) {
      ints.add(i);
    }
    return ints;
  }
}
