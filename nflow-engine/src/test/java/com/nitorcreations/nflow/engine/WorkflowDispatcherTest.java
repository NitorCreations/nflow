package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.inOrder;
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
  }

  @Test
  public void processNextWorkflowInstances() {
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenReturn(asList(1)).thenReturn(new ArrayList<Integer>())
        .thenThrow(new AssertionError());
    try {
      dispatcher.run();
      Assert.fail("Error should stop the dispatcher");
    } catch (AssertionError err) {
      // ok
    }
    verify(repository, times(3)).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory).createExecutor(anyInt());
    verify(pool).execute(any(Runnable.class));
  }

  @Test
  public void shutdownWorks() throws InterruptedException {
    final Thread shutdownThread = new Thread(new Runnable() {
      @Override
      public void run() {
        dispatcher.shutdown();
      }
    });
    when(repository.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new Answer<List<Integer>>() {
      @Override
      public List<Integer> answer(InvocationOnMock invocation) throws Throwable {
        shutdownThread.start();
        List<Integer> ints = new ArrayList<Integer>();
        for (int i = 0; i < 100; i++) {
          ints.add(i);
        }
        return ints;
      }
    });
    dispatcher.run();
    verify(repository).pollNextWorkflowInstanceIds(anyInt());
    verify(executorFactory, times(100)).createExecutor(anyInt());
    InOrder inOrder = inOrder(pool);
    inOrder.verify(pool, times(100)).execute(Mockito.any(Runnable.class));
    inOrder.verify(pool).shutdown();

    shutdownThread.join();
  }

}
