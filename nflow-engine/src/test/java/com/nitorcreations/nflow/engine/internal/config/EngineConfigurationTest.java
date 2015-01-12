package com.nitorcreations.nflow.engine.internal.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;

import com.nitorcreations.nflow.engine.internal.executor.WorkflowInstanceExecutor;

@RunWith(MockitoJUnitRunner.class)
public class EngineConfigurationTest {

  @Spy
  private final MockEnvironment environment = new MockEnvironment().withProperty("nflow.executor.thread.count", "100");
  @Mock
  private ThreadFactory threadFactory;

  @InjectMocks
  private final EngineConfiguration configuration = new EngineConfiguration();

  @Test
  public void dispatcherPoolExecutorInstantiationFromThreads() {
    WorkflowInstanceExecutor executor = configuration.nflowExecutor(threadFactory, environment);
    assertThat(executor.getQueueRemainingCapacity(), is(200));
  }

  @Test
  public void dispatcherPoolExecutorInstantiationFromQueueSize() {
    environment.setProperty("nflow.dispatcher.executor.queue.size", "10");
    WorkflowInstanceExecutor executor = configuration.nflowExecutor(threadFactory, environment);
    assertThat(executor.getQueueRemainingCapacity(), is(10));
  }

  @Test
  public void nonSpringWorkflowsListingNotInstantiated() {
    assertThat(configuration.nflowNonSpringWorkflowsListing(environment), nullValue());
  }
}
