package com.nitorcreations.nflow.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Configures MetricsWorkflowExecutorListener.
 */
@Named("nflowMetrics/metricsContext")
@Configuration
public class NflowMetricsContext {
  private static final Logger logger = LoggerFactory.getLogger(NflowMetricsContext.class);

  @Inject
  private StatisticsService statisticsService;

  @Inject
  private MetricRegistry metricRegistry;

  @Inject
  private HealthCheckRegistry healthCheckRegistry;

  @Bean
  public DatabaseConnectionHealthCheck databaseConnectionHealthCheck() {
    return new DatabaseConnectionHealthCheck(statisticsService);
  }

  @PostConstruct
  public void registerHealthChecks() {
    healthCheckRegistry.register("nflow-database-connection", databaseConnectionHealthCheck());
  }

  @Bean
  public MetricsWorkflowExecutorListener metricsWorkflowExecutorListener(ExecutorDao executors) {
    logger.info("Enabling MetricsWorkflowExecutorListener");
    return new MetricsWorkflowExecutorListener(metricRegistry, executors);
  }

  @Profile("jmx")
  @Bean(destroyMethod="stop")
  public JmxReporter jmxMetricsReporter() {
    logger.info("Enabling Metrics JmxReporter");
    JmxReporter jmxMetricsReporter = JmxReporter.forRegistry(metricRegistry).inDomain("nflow.metrics").build();
    jmxMetricsReporter.start();
    return jmxMetricsReporter;
  }
}
