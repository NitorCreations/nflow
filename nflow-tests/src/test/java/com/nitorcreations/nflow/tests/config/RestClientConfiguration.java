package com.nitorcreations.nflow.tests.config;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nitorcreations.nflow.engine.internal.config.NFlow;

@Configuration
public class RestClientConfiguration {

  @Bean
  @NFlow
  public WebClient baseWebClient(JacksonJsonProvider jsonProvider, Environment env) {
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(env.getRequiredProperty("nflow.url"));
    bean.getFeatures().add(new LoggingFeature());
    bean.setProviders(Arrays.asList(jsonProvider));
    bean.setBus(cxf());
    return bean.createWebClient().type(APPLICATION_JSON).accept(APPLICATION_JSON);
  }

  @Bean(destroyMethod = "shutdown")
  public SpringBus cxf() {
    return new SpringBus();
  }

  @Bean
  public ObjectMapper objectMapper() {
    // this must be kept in sync with the server side (@NFlowRest annotated ObjectMapper bean)
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    return mapper;
  }

  @Bean
  public JacksonJsonProvider jsonProvider(ObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }

  @Bean
  @WorkflowInstance
  public WebClient workflowInstanceWebService(@NFlow WebClient client) {
    return client.path("v1").path("workflow-instance");
  }
}
