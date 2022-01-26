package io.nflow.jetty.config;

import static io.nflow.rest.config.RestConfiguration.REST_OBJECT_MAPPER;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationOutInterceptor;
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
import io.nflow.jetty.mapper.CustomValidationExceptionMapper;
import io.nflow.rest.config.RestConfiguration;
import io.nflow.rest.config.jaxrs.CorsHeaderContainerResponseFilter;
import io.nflow.rest.config.jaxrs.DateTimeParamConverterProvider;
import io.nflow.rest.v1.jaxrs.MaintenanceResource;
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
      StatisticsResource statisticsResource, MaintenanceResource maintenanceResource,
      @Named(REST_OBJECT_MAPPER) ObjectMapper nflowRestObjectMapper, JAXRSBeanValidationInInterceptor validationInInterceptor,
      JAXRSBeanValidationOutInterceptor validationOutInterceptor) {
    JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
    factory.setServiceBeans(Arrays.< Object >asList(
        workflowInstanceResource,
        workflowDefinitionResource,
        workflowExecutorResource,
        statisticsResource,
        maintenanceResource
        ));
    String factoryAddress = factory.getAddress();
    if (!factoryAddress.startsWith("/")) {
      factory.setAddress('/' + factoryAddress);
    }
    factory.setProviders(asList(
        jsonProvider(nflowRestObjectMapper),
        corsHeadersProvider(),
        new WebApplicationExceptionMapper(),
        new CustomValidationExceptionMapper(),
        new DateTimeParamConverterProvider()
        ));
    factory.setFeatures(asList(new LoggingFeature(), swaggerFeature()));
    factory.setBus(cxf());
    factory.setInInterceptors(singletonList(validationInInterceptor));
    factory.setOutInterceptors(singletonList(validationOutInterceptor));
    return factory.create();
  }

  @Bean
  public JAXRSBeanValidationInInterceptor validationInInterceptor() {
    return new JAXRSBeanValidationInInterceptor();
  }

  @Bean
  public JAXRSBeanValidationOutInterceptor validationOutInterceptor() {
    return new JAXRSBeanValidationOutInterceptor();
  }

  private Feature swaggerFeature() {
    OpenApiFeature feature = new OpenApiFeature();
    feature.setScan(true);
    feature.setResourcePackages(singleton(env.getProperty("nflow.swagger.packages", "io.nflow.rest")));
    feature.setContactName("nFlow community");
    feature.setContactEmail("nflow-users@googlegroups.com");
    feature.setContactUrl("https://nflow.io/");
    feature.setDescription(
        "Manage workflow instances, definitions and executors, query statistics and run maintenance jobs. The services are also used by nFlow Explorer.");
    feature.setLicense("European Union Public Licence V. 1.1");
    feature.setLicenseUrl("https://raw.githubusercontent.com/NitorCreations/nflow/master/EUPL-v1.1-Licence.txt");
    feature.setTitle("nFlow REST API");
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
