package com.nitorcreations.nflow.engine.internal.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RunWith(MockitoJUnitRunner.class)
public class EngineConfigurationTest {

  @Spy
  private final MockEnvironment environment = new MockEnvironment().withProperty("nflow.executor.thread.count", "100");
  @Mock
  private ThreadFactory threadFactory;

  @InjectMocks
  private final EngineConfiguration configuration = new EngineConfiguration();

  @Test
  public void dispatcherPoolExecutorInstantiation() {
    ThreadPoolTaskExecutor executor = configuration.nflowExecutor(threadFactory, environment);
    assertThat(executor.getCorePoolSize(), is(100));
    assertThat(executor.getMaxPoolSize(), is(100));
    assertThat(executor.getKeepAliveSeconds(), is(0));
    executor.afterPropertiesSet();
    assertThat(executor.getThreadPoolExecutor().getThreadFactory(), sameInstance(threadFactory));
  }

  @Test
  public void nonSpringWorkflowsListingNotInstantiated() {
    assertThat(configuration.nflowNonSpringWorkflowsListing(environment), nullValue());
  }
}
