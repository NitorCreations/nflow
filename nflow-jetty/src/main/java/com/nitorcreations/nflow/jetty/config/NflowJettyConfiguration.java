package com.nitorcreations.nflow.jetty.config;

import java.util.Arrays;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nitorcreations.nflow.engine.NflowJacksonObjectMapper;
import com.nitorcreations.nflow.jetty.validation.CustomValidationExceptionMapper;
import com.nitorcreations.nflow.rest.config.RestConfiguration;
import com.nitorcreations.nflow.rest.v0.WorkflowDefinitionResource;
import com.nitorcreations.nflow.rest.v0.WorkflowInstanceResource;
import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;

@Configuration
@PropertySource("classpath:nflow-jetty.properties")
@ComponentScan("com.nitorcreations.nflow.jetty")
@Import(value = { RestConfiguration.class, JmxConfiguration.class })
public class NflowJettyConfiguration {

  @Bean
  public Server jaxRsServer(WorkflowInstanceResource workflowInstanceResource, WorkflowDefinitionResource workflowDefinitionResource, NflowJacksonObjectMapper mapper) {
    JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
    factory.setServiceBeans(Arrays.< Object >asList(
        workflowInstanceResource,
        workflowDefinitionResource,
        apiListingResourceJson()));
    factory.setAddress('/' + factory.getAddress());
    factory.setProviders( Arrays.< Object >asList(
        jsonProvider(mapper),
        validationExceptionMapper(),
        resourceListingProvider(),
        apiDeclarationProvider()) );
    factory.setFeatures(Arrays.asList(new LoggingFeature()));
    factory.setBus(cxf());
    factory.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationInInterceptor()));
    factory.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(new JAXRSBeanValidationOutInterceptor()));
    return factory.create();
  }

  @Bean
  public JacksonJsonProvider jsonProvider(NflowJacksonObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }

  @Bean
  public NflowJacksonObjectMapper jsonObjectMapper(Environment env) {
    return new NflowJacksonObjectMapper();
  }

  @Bean
  public CustomValidationExceptionMapper validationExceptionMapper() {
    return new CustomValidationExceptionMapper();
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
  public BeanConfig swaggerConfig(Environment env) {
    final BeanConfig config = new BeanConfig();
    config.setVersion("1.0.0");
    config.setScan(true);
    config.setResourcePackage(WorkflowInstanceResource.class.getPackage().getName());
    config.setBasePath(
        String.format("http://%s:%s",
            env.getProperty("swagger.server", "localhost"),
            env.getProperty("swagger.port", "7500")
    ));
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

}
