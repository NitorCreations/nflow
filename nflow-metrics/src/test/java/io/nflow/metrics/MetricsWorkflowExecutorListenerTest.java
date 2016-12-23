package io.nflow.metrics;

import static io.nflow.engine.internal.config.Profiles.H2;
import static io.nflow.engine.internal.config.Profiles.JMX;
import static io.nflow.engine.internal.config.Profiles.METRICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.internal.config.Profiles;
import io.nflow.engine.internal.storage.db.H2DatabaseConfiguration;
import io.nflow.engine.internal.storage.db.SQLVariants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.service.HealthCheckService;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.metrics.MetricsWorkflowExecutorListener;
import io.nflow.metrics.NflowMetricsContext;


public class MetricsWorkflowExecutorListenerTest {

  AnnotationConfigApplicationContext ctx;
  MetricRegistry metricRegistry;
  MetricsWorkflowExecutorListener listener;
  WorkflowDefinition<?> definition = mock(WorkflowDefinition.class);
  WorkflowInstance instance = new WorkflowInstance.Builder().setRetries(2).setState("my-state").setNextActivation(null).build();
  WorkflowInstance instance2 = new WorkflowInstance.Builder().setRetries(2).setState("my-state").build();
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
        env.addActiveProfile(METRICS);
        env.addActiveProfile(JMX);
        env.addActiveProfile(H2);
        return env;
      }
    };
    metricRegistry = ctx.getBean(MetricRegistry.class);
    listener = ctx.getBean(MetricsWorkflowExecutorListener.class);
    when(definition.getType()).thenReturn("myWorkflow");
  }

  @Test
  public void whenNextActivationIsSetToNullBeforeContext() {
    listener.beforeProcessing(context);
    assertEquals(1, metricRegistry.getHistograms().get("foobarName.0.myWorkflow.my-state.retries").getCount());
    assertNotNull(metricRegistry.getTimers().get("foobarName.0.myWorkflow.my-state.execution-time"));
    assertNull(metricRegistry.getHistograms().get("foobarName.0.startup-delay"));
  }

  @Test
  public void beforeContext() {
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
    public HealthCheckService healthCheckService() {
      return mock(HealthCheckService.class);
    }

    @Bean
    public ExecutorDao executorDao() {
      ExecutorDao dao = mock(ExecutorDao.class);
      when(dao.getExecutorGroup()).thenReturn("foobarName");
      when(dao.getExecutorId()).thenReturn(0);
      return dao;
    }

    @Bean
    public SQLVariants SQLVariants() {
      return mock(SQLVariants.class);
    }

    @Bean
    @NFlow
    public JdbcTemplate jdbcTemplate() {
      return mock(JdbcTemplate.class);
    }
  }
}
