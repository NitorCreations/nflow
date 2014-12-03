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

import com.nitorcreations.nflow.engine.internal.executor.ThresholdThreadPoolExecutor;

@RunWith(MockitoJUnitRunner.class)
public class EngineConfigurationTest {

  @Spy
  private final MockEnvironment environment = new MockEnvironment().withProperty("nflow.executor.thread.count", "100");
  @Mock
  private ThreadFactory threadFactory;

  @InjectMocks
  private final EngineConfiguration configuration = new EngineConfiguration();

  public void dispatcherPoolExecutorInstantiation() {
    ThresholdThreadPoolExecutor executor = configuration.nflowExecutor(threadFactory, environment);
    assertThat(executor.getMaximumPoolSize(), is(100));
  }

  @Test
  public void nonSpringWorkflowsListingNotInstantiated() {
    assertThat(configuration.nflowNonSpringWorkflowsListing(environment), nullValue());
  }
}
