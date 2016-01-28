package com.nitorcreations.nflow.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.nitorcreations.nflow.engine.internal.dao.StatisticsDao;
import com.nitorcreations.nflow.engine.service.StatisticsService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import com.codahale.metrics.MetricRegistry;
import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;


public class MetricsWorkflowExecutorListenerTest {

  AnnotationConfigApplicationContext ctx;
  MetricRegistry metricRegistry;
  MetricsWorkflowExecutorListener listener;
  WorkflowDefinition<?> definition = mock(WorkflowDefinition.class);
  WorkflowInstance instance = new WorkflowInstance.Builder().setRetries(2).setState("my-state").build();
  WorkflowInstance instance2 = new WorkflowInstance.Builder().setRetries(2).setState("my-state")
      .setNextActivation(DateTime.now()).build();
  StateExecution stateExecution = mock(StateExecution.class);
  ListenerContext context = new WorkflowExecutorListener.ListenerContext(
      definition, instance, stateExecution);

  ListenerContext context2 = new WorkflowExecutorListener.ListenerContext(
      definition, instance2, stateExecution);
  @Before
  public void setup() {
    ctx = new AnnotationConfigApplicationContext(Config.class) {
      @Override
      protected ConfigurableEnvironment createEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.addActiveProfile("metrics");
        env.addActiveProfile("jmx");
        return env;
      }
    };
    metricRegistry = ctx.getBean(MetricRegistry.class);
    listener = ctx.getBean(MetricsWorkflowExecutorListener.class);
    when(definition.getType()).thenReturn("myWorkflow");
  }

  @Test
  public void beforeContext() {
    listener.beforeProcessing(context);
    assertEquals(1, metricRegistry.getHistograms().get("foobarName.0.myWorkflow.my-state.retries").getCount());
    assertNotNull(metricRegistry.getTimers().get("foobarName.0.myWorkflow.my-state.execution-time"));
    assertNull(metricRegistry.getHistograms().get("foobarName.0.startup-delay"));
  }

  @Test
  public void whenNextActivationIsSetBeforeContext() {
    listener.beforeProcessing(context2);
    assertEquals(1, metricRegistry.getHistograms().get("foobarName.0.myWorkflow.my-state.retries").getCount());
    assertNotNull(metricRegistry.getTimers().get("foobarName.0.myWorkflow.my-state.execution-time"));
    assertNotNull(metricRegistry.getHistograms().get("foobarName.0.startup-delay"));
  }

  @Test
  public void afterSuccess() {
    listener.beforeProcessing(context);
    listener.afterProcessing(context);
    assertNotNull(metricRegistry.getHistograms().get("foobarName.0.myWorkflow.my-state.retries"));
    assertEquals(1, metricRegistry.getTimers().get("foobarName.0.myWorkflow.my-state.execution-time").getCount());
    assertEquals(1, metricRegistry.getMeters().get("foobarName.0.myWorkflow.my-state.success-count").getCount());
  }

  @Test
  public void afterFailure() {
    listener.beforeProcessing(context);
    listener.afterFailure(context, new Exception());
    assertNotNull(metricRegistry.getHistograms().get("foobarName.0.myWorkflow.my-state.retries"));
    assertEquals(1, metricRegistry.getTimers().get("foobarName.0.myWorkflow.my-state.execution-time").getCount());
    assertEquals(1, metricRegistry.getMeters().get("foobarName.0.myWorkflow.my-state.error-count").getCount());
  }

  @Configuration
  @Import(NflowMetricsContext.class)
  public static class Config {
    @Bean
    public MetricRegistry metricRegistry() {
      return new MetricRegistry();
    }

    @Bean
    public HealthCheckRegistry healthCheckRegistry() {
      return new HealthCheckRegistry();
    }

    @Bean
    public StatisticsService statisticsService() {
      return mock(StatisticsService.class);
    }

    @Bean
    public StatisticsDao statisticsDao() {
      return mock(StatisticsDao.class);
    }

    @Bean
    public ExecutorDao executorDao() {
      ExecutorDao dao = mock(ExecutorDao.class);
      when(dao.getExecutorGroup()).thenReturn("foobarName");
      when(dao.getExecutorId()).thenReturn(0);
      return dao;
    }
  }
}
