package io.nflow.tests.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Arrays.asList;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import jakarta.inject.Inject;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

@Configuration
public class RestClientConfiguration {

  @Inject
  private JacksonJsonProvider jsonProvider;

  @Inject
  Environment env;

  @SuppressWarnings("resource")
  @Scope(value = SCOPE_PROTOTYPE)
  public WebClient baseWebClient() {
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(env.getRequiredProperty("nflow.url"));
    bean.getFeatures().add(new LoggingFeature());
    bean.setProviders(asList(jsonProvider));
    bean.setBus(cxf());
    return bean.createWebClient().type(APPLICATION_JSON).accept(APPLICATION_JSON).path("nflow").path("api").path("v1");
  }

  @Bean(destroyMethod = "shutdown")
  public SpringBus cxf() {
    return new SpringBus();
  }

  @Bean
  public JsonMapper objectMapper() {
    // this must be kept in sync with the server side (nflowRestObjectMapper)
    return JsonMapper.builder()
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(NON_EMPTY))
        .addModule(new JodaModule())
        .build();
  }

  @Bean
  public JacksonJsonProvider jsonProvider(JsonMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }

  @SuppressWarnings("resource")
  @Bean(name = "workflowInstance")
  public WebClient workflowInstance() {
    return baseWebClient().path("workflow-instance");
  }

  @SuppressWarnings("resource")
  @Bean(name = "workflowInstanceId")
  public WebClient workflowInstanceId() {
    return baseWebClient().path("workflow-instance/id");
  }

  @SuppressWarnings("resource")
  @Bean(name = "workflowDefinition")
  public WebClient workflowDefinition() {
    return baseWebClient().path("workflow-definition");
  }

  @SuppressWarnings("resource")
  @Bean(name = "statistics")
  public WebClient statistics() {
    return baseWebClient().path("statistics");
  }

  @SuppressWarnings("resource")
  @Bean(name = "maintenance")
  public WebClient maintenance() {
    return baseWebClient().path("maintenance");
  }

  @SuppressWarnings("resource")
  @Bean(name = "metrics")
  public WebClient metrics() {
    return baseWebClient().back(true).path("nflow").path("metrics");
  }
}
