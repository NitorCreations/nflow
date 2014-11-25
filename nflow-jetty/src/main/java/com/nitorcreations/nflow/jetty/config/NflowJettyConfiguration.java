package com.nitorcreations.nflow.jetty.config;

import static com.nitorcreations.nflow.jetty.StartNflow.DEFAULT_HOST;
import static com.nitorcreations.nflow.jetty.StartNflow.DEFAULT_PORT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationOutInterceptor;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nitorcreations.nflow.jetty.validation.CustomValidationExceptionMapper;
import com.nitorcreations.nflow.rest.config.BadRequestExceptionMapper;
import com.nitorcreations.nflow.rest.config.CorsHeaderContainerResponseFilter;
import com.nitorcreations.nflow.rest.config.DateTimeParamConverterProvider;
import com.nitorcreations.nflow.rest.config.NotFoundExceptionMapper;
import com.nitorcreations.nflow.rest.config.RestConfiguration;
import com.nitorcreations.nflow.rest.v1.WorkflowDefinitionResource;
import com.nitorcreations.nflow.rest.v1.WorkflowExecutorResource;
import com.nitorcreations.nflow.rest.v1.WorkflowInstanceResource;
import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;

@Configuration
@PropertySource("classpath:nflow-jetty.properties")
@ComponentScan("com.nitorcreations.nflow.jetty")
@Import(value = { RestConfiguration.class, JmxConfiguration.class})
@EnableTransactionManagement
public class NflowJettyConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(NflowJettyConfiguration.class);

  @Inject
  Environment env;

  @Bean
  public Server jaxRsServer(WorkflowInstanceResource workflowInstanceResource,
      WorkflowDefinitionResource workflowDefinitionResource, WorkflowExecutorResource workflowExecutorResource,
      @Named("nflowRestObjectMapper") ObjectMapper mapper) {
    JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
    factory.setServiceBeans(Arrays.< Object >asList(
        workflowInstanceResource,
        workflowDefinitionResource,
        workflowExecutorResource,
        apiListingResourceJson()));
    factory.setAddress('/' + factory.getAddress());
    factory.setProviders( Arrays.asList(
        jsonProvider(mapper),
        validationExceptionMapper(),
        resourceListingProvider(),
        apiDeclarationProvider(),
        corsHeadersProvider(),
        notFoundExceptionMapper(),
        new BadRequestExceptionMapper(),
        new DateTimeParamConverterProvider()
        ));
    factory.setFeatures(Arrays.asList(new LoggingFeature()));
    factory.setBus(cxf());
    factory.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationInInterceptor()));
    factory.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationOutInterceptor()));
    return factory.create();
  }

  private CorsHeaderContainerResponseFilter corsHeadersProvider() {
    return new CorsHeaderContainerResponseFilter(env);
  }

  @Bean
  public JacksonJsonProvider jsonProvider(@Named("nflowRestObjectMapper") ObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
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

  @Bean
  public BeanConfig swaggerConfig() {
    final BeanConfig config = new BeanConfig();
    config.setVersion("1.0.0");
    config.setScan(true);
    config.setResourcePackage(WorkflowInstanceResource.class.getPackage().getName());
    String basePath = env.getProperty("swagger.basepath");
    if (isEmpty(basePath)) {
      basePath = String.format("%s://%s:%d%s",
          env.getProperty("swagger.basepath.protocol", "http"),
          env.getProperty("swagger.basepath.server", env.getProperty("host", DEFAULT_HOST)),
          env.getProperty("swagger.basepath.port", Integer.class, env.getProperty("port", Integer.class, DEFAULT_PORT)),
          env.getProperty("swagger.basepath.context", "/api"));
    }
    logger.debug("Swagger basepath: {}", basePath);
    config.setBasePath(basePath);
    return config;
  }

  @Bean
  public ApiDeclarationProvider apiDeclarationProvider() {
   return new ApiDeclarationProvider();
  }

  @Bean
  public ApiListingResourceJSON apiListingResourceJson() {
   return new ApiListingResourceJSON();
  }

  @Bean
  public ResourceListingProvider resourceListingProvider() {
   return new ResourceListingProvider();
  }

  @ApplicationPath("/")
  public static class JaxRsApiApplication extends Application {
  }

  @Bean
  public PlatformTransactionManager transactionManager(@Named("nflowDatasource") DataSource dataSource)  {
    return new DataSourceTransactionManager(dataSource);
  }
}
