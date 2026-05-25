package io.nflow.performance.client;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.singletonList;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import jakarta.inject.Inject;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

@Configuration
@ComponentScan("io.nflow.performance")
public class PerfTestConfiguration {

  @Inject
  private JacksonJsonProvider jsonProvider;

  @Inject
  Environment env;

  @SuppressWarnings("resource")
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
  public JsonMapper jsonMapper() {
    // this must be kept in sync with the server side (nflowRestJsonMapper)
    return JsonMapper.builder()
        .configure(WRITE_DATES_AS_TIMESTAMPS, false)
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(NON_EMPTY))
        .addModule(new JodaModule())
        .enable(FAIL_ON_TRAILING_TOKENS)
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
  @Bean(name = "statistics")
  public WebClient statistics() {
    return baseWebClient().path("statistics");
  }
}
