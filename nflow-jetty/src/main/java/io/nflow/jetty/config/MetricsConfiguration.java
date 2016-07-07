package io.nflow.jetty.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
public class MetricsConfiguration {

  @Bean
  public MetricRegistry metricRegistry() {
    return new MetricRegistry();
  }

  @Bean
  public HealthCheckRegistry healthCheckRegistry() {
    return new HealthCheckRegistry();
  }

  @PostConstruct
  public void registerHealthChecks() {
    healthCheckRegistry().register("threadDeadlocks", new ThreadDeadlockHealthCheck());
  }

  @PostConstruct
  public void registerMetrics() {
    metricRegistry().register("memoryUsage", new MemoryUsageGaugeSet());
    metricRegistry().register("bufferPools", new BufferPoolMetricSet( ManagementFactory.getPlatformMBeanServer()));
    metricRegistry().register("garbageCollector", new GarbageCollectorMetricSet());
    metricRegistry().register("classLoading", new ClassLoadingGaugeSet());
    metricRegistry().register("fileDescriptorRatio", new FileDescriptorRatioGauge());
    metricRegistry().register("threadStates", new CachedThreadStatesGaugeSet(ManagementFactory.getThreadMXBean(), new ThreadDeadlockDetector(), 60, SECONDS));
  }
}
