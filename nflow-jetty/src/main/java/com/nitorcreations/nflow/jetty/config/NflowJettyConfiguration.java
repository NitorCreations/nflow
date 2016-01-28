package com.nitorcreations.nflow.jetty.config;

import static com.nitorcreations.nflow.rest.config.RestConfiguration.REST_OBJECT_MAPPER;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.*;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationOutInterceptor;
import org.apache.cxf.message.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.jetty.validation.CustomValidationExceptionMapper;
import com.nitorcreations.nflow.rest.config.BadRequestExceptionMapper;
import com.nitorcreations.nflow.rest.config.CorsHeaderContainerResponseFilter;
import com.nitorcreations.nflow.rest.config.DateTimeParamConverterProvider;
import com.nitorcreations.nflow.rest.config.NotFoundExceptionMapper;
import com.nitorcreations.nflow.rest.config.RestConfiguration;
import com.nitorcreations.nflow.rest.v1.ArchiveResource;
import com.nitorcreations.nflow.rest.v1.StatisticsResource;
import com.nitorcreations.nflow.rest.v1.WorkflowDefinitionResource;
import com.nitorcreations.nflow.rest.v1.WorkflowExecutorResource;
import com.nitorcreations.nflow.rest.v1.WorkflowInstanceResource;

@Configuration
@ComponentScan("com.nitorcreations.nflow.jetty")
@Import(value = { RestConfiguration.class, JmxConfiguration.class})
@EnableTransactionManagement
public class NflowJettyConfiguration {

  @Inject
  Environment env;

  @Bean
  public Server jaxRsServer(WorkflowInstanceResource workflowInstanceResource,
      WorkflowDefinitionResource workflowDefinitionResource, WorkflowExecutorResource workflowExecutorResource,
      StatisticsResource statisticsResource, ArchiveResource archiveResource,
      @Named(REST_OBJECT_MAPPER) ObjectMapper nflowRestObjectMapper) {
    JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
    factory.setServiceBeans(Arrays.< Object >asList(
        workflowInstanceResource,
        workflowDefinitionResource,
        workflowExecutorResource,
        statisticsResource,
        archiveResource
        ));
    factory.setAddress('/' + factory.getAddress());
    factory.setProviders(asList(
        jsonProvider(nflowRestObjectMapper),
        validationExceptionMapper(),
        corsHeadersProvider(),
        notFoundExceptionMapper(),
        new BadRequestExceptionMapper(),
        new DateTimeParamConverterProvider()
        ));
    factory.setFeatures(asList(new LoggingFeature()));
    factory.setBus(cxf());
    factory.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationInInterceptor()));
    factory.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationOutInterceptor()));
    return factory.create();
  }

  private CorsHeaderContainerResponseFilter corsHeadersProvider() {
    return new CorsHeaderContainerResponseFilter(env);
  }

  @Bean
  public JacksonJsonProvider jsonProvider(@Named(REST_OBJECT_MAPPER) ObjectMapper nflowRestObjectMapper) {
    return new JacksonJsonProvider(nflowRestObjectMapper);
  }

  @Bean
  public CustomValidationExceptionMapper validationExceptionMapper() {
    return new CustomValidationExceptionMapper();
  }

  @Bean
  public NotFoundExceptionMapper notFoundExceptionMapper() {
    return new NotFoundExceptionMapper();
  }

  @Bean(destroyMethod = "shutdown")
  public SpringBus cxf() {
    return new SpringBus();
  }

  @Bean
  public JaxRsApiApplication jaxRsApiApplication() {
      return new JaxRsApiApplication();
  }

  @ApplicationPath("/")
  public static class JaxRsApiApplication extends Application {
  }

  @Bean
  public PlatformTransactionManager transactionManager(@NFlow DataSource nflowDataSource)  {
    return new DataSourceTransactionManager(nflowDataSource);
  }

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
    healthCheckRegistry().register("thread-deadlocks", new ThreadDeadlockHealthCheck());
  }

  @PostConstruct
  public void registerMetrics() {
    metricRegistry().register("memory-usage", new MemoryUsageGaugeSet());
    metricRegistry().register("buffer-pools", new BufferPoolMetricSet( ManagementFactory.getPlatformMBeanServer()));
    metricRegistry().register("garbage-collector", new GarbageCollectorMetricSet());
    metricRegistry().register("class-loading", new ClassLoadingGaugeSet());
    metricRegistry().register("file-descriptor-ratio", new FileDescriptorRatioGauge());
    metricRegistry().register("thread-states", new CachedThreadStatesGaugeSet(ManagementFactory.getThreadMXBean(), new ThreadDeadlockDetector(), 60, SECONDS));
  }
}
