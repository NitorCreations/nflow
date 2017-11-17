package io.nflow.jetty.config;

import static io.nflow.rest.config.RestConfiguration.REST_OBJECT_MAPPER;
import static java.util.Arrays.asList;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
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

import io.nflow.engine.config.NFlow;
import io.nflow.jetty.mapper.BadRequestExceptionMapper;
import io.nflow.jetty.mapper.CustomValidationExceptionMapper;
import io.nflow.jetty.mapper.NotFoundExceptionMapper;
import io.nflow.rest.config.RestConfiguration;
import io.nflow.rest.config.jaxrs.CorsHeaderContainerResponseFilter;
import io.nflow.rest.config.jaxrs.DateTimeParamConverterProvider;
import io.nflow.rest.v1.jaxrs.ArchiveResource;
import io.nflow.rest.v1.jaxrs.StatisticsResource;
import io.nflow.rest.v1.jaxrs.WorkflowDefinitionResource;
import io.nflow.rest.v1.jaxrs.WorkflowExecutorResource;
import io.nflow.rest.v1.jaxrs.WorkflowInstanceResource;

@Configuration
@ComponentScan("io.nflow.jetty")
@Import(value = { RestConfiguration.class, JmxConfiguration.class, MetricsConfiguration.class})
@EnableTransactionManagement
public class NflowJettyConfiguration {

  @Inject
  private Environment env;

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
    factory.setAddress(env.getProperty("nflow.api.basepath", '/' + factory.getAddress()));
    factory.setProviders(asList(
        jsonProvider(nflowRestObjectMapper),
        validationExceptionMapper(),
        corsHeadersProvider(),
        notFoundExceptionMapper(),
        new BadRequestExceptionMapper(),
        new DateTimeParamConverterProvider()
        ));
    factory.setFeatures(asList(new LoggingFeature(), swaggerFeature()));
    factory.setBus(cxf());
    factory.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationInInterceptor()));
    factory.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationOutInterceptor()));
    return factory.create();
  }

  private Feature swaggerFeature() {
    Swagger2Feature feature = new Swagger2Feature();
    feature.setBasePath(env.getProperty("nflow.swagger.basepath", "/api"));
    feature.setContact("nFlow community (nflow-users@googlegroups.com)");
    feature.setDescription(
        "nFlow REST API provides services for managing workflow instances and querying metadata (statistics, workflow "
            + "definitions, etc) of nFlow Engine. The services are also used by nFlow Explorer user interface.");
    feature.setLicense("European Union Public Licence V. 1.1");
    feature.setLicenseUrl("https://raw.githubusercontent.com/NitorCreations/nflow/master/EUPL-v1.1-Licence.txt");
    feature.setTitle("nflow-rest-api");
    feature.setVersion("1");
    return feature;
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

}
