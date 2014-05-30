package com.nitorcreations.nflow.metrics;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.codahale.metrics.MetricRegistry;

/**
 * Configures MetricsWorkflowExecutorListener.
 */
@Named("nflow-metrics/metricsContext")
@Configuration
public class NflowMetricsContext {
  @Inject
  private Environment env;
  @Inject
  private MetricRegistry metricRegistry;

  @Bean
  public MetricsWorkflowExecutorListener metricsWorkflowExecutorListener() {
    return new MetricsWorkflowExecutorListener(metricRegistry, env);
  }
}
