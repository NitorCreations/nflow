package com.nitorcreations.nflow.tests.config;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nitorcreations.nflow.jetty.config.JacksonObjectMapper;

@Configuration
public class RestClientConfiguration {

  @Inject
  Environment env;

  @Bean(name="base-webclient")
  public WebClient baseWebClient(JacksonJsonProvider jsonProvider) {
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(env.getRequiredProperty("nflow.url"));
    bean.getFeatures().add(new LoggingFeature());
    bean.setProviders(Arrays.asList(jsonProvider));
    bean.setBus(cxf());
    return bean.createWebClient().type(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
  }

  @Bean(destroyMethod = "shutdown")
  public SpringBus cxf() {
    return new SpringBus();
  }

  @Bean
  public JacksonJsonProvider jsonProvider(JacksonObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }

  @Bean
  public JacksonObjectMapper jsonObjectMapper(Environment env) {
    return new JacksonObjectMapper();
  }

  @Bean(name="workflow-instance")
  public WebClient workflowInstanceWebService(@Named("base-webclient") WebClient client) {
    return client.path("v0").path("workflow-instance");
  }

}
