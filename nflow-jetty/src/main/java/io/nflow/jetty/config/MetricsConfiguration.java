package io.nflow.jetty.config;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.util.concurrent.TimeUnit.SECONDS;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;

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
    MetricRegistry registry = metricRegistry();
    registry.register("memoryUsage", new MemoryUsageGaugeSet());
    registry.register("bufferPools", new BufferPoolMetricSet(getPlatformMBeanServer()));
    registry.register("garbageCollector", new GarbageCollectorMetricSet());
    registry.register("classLoading", new ClassLoadingGaugeSet());
    registry.register("fileDescriptorRatio", new FileDescriptorRatioGauge());
    registry.register("threadStates",
        new CachedThreadStatesGaugeSet(getThreadMXBean(), new ThreadDeadlockDetector(), 60, SECONDS));
  }
}
