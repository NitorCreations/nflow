package com.nitorcreations.nflow.engine.internal.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadFactory;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.internal.executor.WorkflowInstanceExecutor;

@RunWith(MockitoJUnitRunner.class)
public class EngineConfigurationTest {

  @Spy
  private final MockEnvironment environment = new MockEnvironment().withProperty("nflow.executor.thread.count", "100")
      .withProperty("nflow.dispatcher.await.termination.seconds", "60")
      .withProperty("nflow.dispatcher.executor.thread.keepalive.seconds", "0");
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

  @Test
  public void nonSpringWorkflowsListingInstantiationAttempted() {
    environment.withProperty("nflow.non_spring_workflows_filename", "dummy");
    assertThat(configuration.nflowNonSpringWorkflowsListing(environment), notNullValue());
  }

  @Test
  public void nflowThreadFactoryInstantiated() {
    ThreadFactory factory = configuration.nflowThreadFactory();
    assertThat(factory, instanceOf(CustomizableThreadFactory.class));
    assertThat(((CustomizableThreadFactory) factory).getThreadNamePrefix(), is("nflow-executor-"));
    assertThat(((CustomizableThreadFactory) factory).getThreadGroup().getName(), is("nflow"));
  }

  @Test
  public void nflowObjectMapperInstantiated() {
    ObjectMapper mapper = configuration.nflowObjectMapper();
    assertThat(mapper.canSerialize(DateTime.class), is(true));
    assertThat(mapper.getSerializationConfig().getSerializationInclusion(), is(JsonInclude.Include.NON_EMPTY));
  }
}
