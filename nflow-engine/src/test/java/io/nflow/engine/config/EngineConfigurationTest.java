package io.nflow.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.spring.EngineConfiguration;

@ExtendWith(MockitoExtension.class)
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
    //TODO WorkflowInstanceExecutor executor = configuration.nflowExecutor(threadFactory, environment);
    // assertThat(executor.getQueueRemainingCapacity(), is(200));
  }

  @Test
  public void dispatcherPoolExecutorInstantiationFromQueueSize() {
    environment.setProperty("nflow.dispatcher.executor.queue.size", "10");
// TODO    WorkflowInstanceExecutor executor = configuration.nflowExecutor(threadFactory, environment);
//    assertThat(executor.getQueueRemainingCapacity(), is(10));
  }

  @Test
  public void nonSpringWorkflowsListingNotInstantiated() throws IOException {
    assertEquals(configuration.nflowNonSpringWorkflowsListing(environment).contentLength(), 0L);
  }

  @Test
  public void nonSpringWorkflowsListingInstantiationAttempted() {
    environment.withProperty("nflow.non_spring_workflows_filename", "dummy");
    assertEquals(configuration.nflowNonSpringWorkflowsListing(environment).getFilename(), "dummy");
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
    assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion(),
        is(JsonInclude.Include.NON_EMPTY));
  }
}
