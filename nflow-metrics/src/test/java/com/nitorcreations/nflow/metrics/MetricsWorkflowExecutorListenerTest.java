package com.nitorcreations.nflow.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import com.codahale.metrics.MetricRegistry;
import com.nitorcreations.nflow.engine.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;


public class MetricsWorkflowExecutorListenerTest {

  AnnotationConfigApplicationContext ctx;
  MetricRegistry metricRegistry;
  MetricsWorkflowExecutorListener listener;
  WorkflowDefinition<?> definition = mock(WorkflowDefinition.class);
  WorkflowInstance instance = new WorkflowInstance.Builder().setRetries(2).setState("my-state").build();
  StateExecution stateExecution = mock(StateExecution.class);
  ListenerContext context = new WorkflowExecutorListener.ListenerContext(
      definition, instance, stateExecution);

  @Before
  public void setup() {
    ctx = new AnnotationConfigApplicationContext(Config.class);
    metricRegistry = ctx.getBean(MetricRegistry.class);
    listener = ctx.getBean(MetricsWorkflowExecutorListener.class);
    when(definition.getName()).thenReturn("myWorkflow");
  }

  @Test
  public void beforeContext() {
    listener.beforeProcessing(context);
    assertEquals(1, metricRegistry.getHistograms().get("nflow.foobarName.myWorkflow.my-state.retries").getCount());
    assertNotNull(metricRegistry.getTimers().get("nflow.foobarName.myWorkflow.my-state.execution-time"));
  }

  @Test
  public void afterSuccess() {
    listener.beforeProcessing(context);
    listener.afterProcessing(context);
    assertNotNull(metricRegistry.getHistograms().get("nflow.foobarName.myWorkflow.my-state.retries"));
    assertEquals(1, metricRegistry.getTimers().get("nflow.foobarName.myWorkflow.my-state.execution-time").getCount());
    assertEquals(1, metricRegistry.getMeters().get("nflow.foobarName.myWorkflow.my-state.success-count").getCount());
  }

  @Test
  public void afterFailure() {
    listener.beforeProcessing(context);
    listener.afterFailure(context, new Exception());
    assertNotNull(metricRegistry.getHistograms().get("nflow.foobarName.myWorkflow.my-state.retries"));
    assertEquals(1, metricRegistry.getTimers().get("nflow.foobarName.myWorkflow.my-state.execution-time").getCount());
    assertEquals(1, metricRegistry.getMeters().get("nflow.foobarName.myWorkflow.my-state.error-count").getCount());
  }

  @Configuration
  @Import(NflowMetricsContext.class)
  static class Config {
    @Bean
    public Environment env() {
      MockEnvironment env = new MockEnvironment();
      env.setProperty("nflow.instance.name", "foobarName");
      return env;
    }
    @Bean
    public MetricRegistry metricRegistry() {
      return new MetricRegistry();
    }
  }
}
