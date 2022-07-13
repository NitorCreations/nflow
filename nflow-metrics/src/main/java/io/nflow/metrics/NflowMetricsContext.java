package io.nflow.metrics;

import static io.nflow.engine.config.Profiles.JMX;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.codahale.metrics.DefaultSettableGauge;
import io.nflow.engine.config.db.DatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jmx.JmxReporter;

import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.service.HealthCheckService;

/**
 * Configures MetricsWorkflowExecutorListener.
 */
@Named("nflowMetrics/metricsContext")
@Configuration
public class NflowMetricsContext {
  private static final Logger logger = LoggerFactory.getLogger(NflowMetricsContext.class);

  @Inject
  private HealthCheckService healthCheckService;

  @Inject
  private MetricRegistry metricRegistry;

  @Inject
  private HealthCheckRegistry healthCheckRegistry;

  @Inject
  private DatabaseConfiguration databaseConfiguration;

  @Bean
  public DatabaseConnectionHealthCheck databaseConnectionHealthCheck() {
    return new DatabaseConnectionHealthCheck(healthCheckService);
  }

  @PostConstruct
  public void registerHealthChecks() {
    healthCheckRegistry.register("nflowDatabaseConnection", databaseConnectionHealthCheck());
  }

  @PostConstruct
  public void registerDatabaseType() {
    metricRegistry.gauge("nflow.database.type", () -> new DefaultSettableGauge<>(databaseConfiguration.getDbType()));
  }

  @Bean
  public MetricsWorkflowExecutorListener metricsWorkflowExecutorListener(ExecutorDao executors) {
    logger.info("Enabling MetricsWorkflowExecutorListener");
    return new MetricsWorkflowExecutorListener(metricRegistry, executors);
  }

  @Profile(JMX)
  @Bean(destroyMethod="stop")
  public JmxReporter jmxMetricsReporter() {
    logger.info("Enabling Metrics JmxReporter");
    JmxReporter jmxMetricsReporter = JmxReporter.forRegistry(metricRegistry).inDomain("nflow.metrics").build();
    jmxMetricsReporter.start();
    return jmxMetricsReporter;
  }
}
