package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
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

  @After
  public void cleanup() {
    dispatcher.shutdown();
  }

  @Test
  public void processNextWorkflowInstances() {
    when(repository.pollNextWorkflowInstanceIds(Mockito.anyInt())).thenReturn(asList(1)).thenReturn(new ArrayList<Integer>())
        .thenThrow(new AssertionError());
    try {
      dispatcher.run();
      Assert.fail("Error should stop the dispatcher");
    } catch (AssertionError err) {
      // ok
    }
    verify(repository, times(3)).pollNextWorkflowInstanceIds(Mockito.anyInt());
    verify(executorFactory).createExecutor(Mockito.anyInt());
    verify(pool).execute(Mockito.any(Runnable.class));
  }

}
