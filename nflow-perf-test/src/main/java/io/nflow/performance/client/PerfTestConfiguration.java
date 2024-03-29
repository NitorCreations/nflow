package io.nflow.performance.client;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.util.Collections.singletonList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import jakarta.inject.Inject;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Configuration
@ComponentScan("io.nflow.performance")
public class PerfTestConfiguration {

  @Inject
  private JacksonJsonProvider jsonProvider;

  @Inject
  Environment env;

  @Scope(value = SCOPE_PROTOTYPE)
  public WebClient baseWebClient() {
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(env.getProperty("nflow.url", "http://localhost:7500"));
    bean.getFeatures().add(new LoggingFeature());
    bean.setProviders(singletonList(jsonProvider));
    bean.setBus(cxf());
    return bean.createWebClient().type(APPLICATION_JSON).accept(APPLICATION_JSON).path("api").path("v1");
  }

  @Bean(destroyMethod = "shutdown")
  public SpringBus cxf() {
    return new SpringBus();
  }

  @Bean
  public ObjectMapper objectMapper() {
    // this must be kept in sync with the server side (nflowRestObjectMapper)
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
    mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    return mapper;
  }

  @Bean
  public JacksonJsonProvider jsonProvider(ObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }

  @Bean(name = "workflowInstance")
  public WebClient workflowInstance() {
    return baseWebClient().path("workflow-instance");
  }

  @Bean(name="statistics")
  public WebClient statistics() {
    return baseWebClient().path("statistics");
  }
}
