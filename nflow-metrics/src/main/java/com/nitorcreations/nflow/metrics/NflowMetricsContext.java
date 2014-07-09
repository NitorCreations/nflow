package com.nitorcreations.nflow.metrics;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * Configures MetricsWorkflowExecutorListener.
 */
@Named("nflow-metrics/metricsContext")
@Configuration
public class NflowMetricsContext {
  private static final Logger log = LoggerFactory.getLogger(NflowMetricsContext.class);
  @Inject
  private Environment env;

  @Bean
  public MetricRegistry metricRegistry() {
    return new MetricRegistry();
  }

  @Bean
  public MetricsWorkflowExecutorListener metricsWorkflowExecutorListener() {
    log.info("Enabling MetricsWorkflowExecutorListener");
    return new MetricsWorkflowExecutorListener(metricRegistry(), env);
  }

  @Profile("jmx")
  @Bean(destroyMethod="stop")
  public JmxReporter jmxMetricsReporter() {
    log.info("Enabling Metrics JmxReporter");
    JmxReporter jmxMetricsReporter = JmxReporter.forRegistry(metricRegistry()).inDomain("nflow.metrics").build();
    jmxMetricsReporter.start();
    return jmxMetricsReporter;
  }
}
