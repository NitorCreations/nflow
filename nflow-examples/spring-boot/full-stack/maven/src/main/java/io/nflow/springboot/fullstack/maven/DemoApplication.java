package io.nflow.springboot.fullstack.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.rest.config.NflowRestApiPropertiesConfiguration;
import io.nflow.rest.config.RestConfiguration;
import jakarta.inject.Inject;

import jakarta.inject.Named;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static io.nflow.rest.config.RestConfiguration.REST_OBJECT_MAPPER;

@SpringBootApplication
@Import({ EngineConfiguration.class, NflowRestApiPropertiesConfiguration.class })
@ComponentScan(value = "io.nflow.rest", excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RestConfiguration.class))
public class DemoApplication {

  @Inject
  private WorkflowInstanceService workflowInstances;

  @Inject
  private WorkflowInstanceFactory workflowInstanceFactory;

  @EventListener(ApplicationReadyEvent.class)
  public void insertWorkflowInstance() {
    workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
        .setType(ExampleWorkflow.TYPE)
        .setExternalId("example")
        .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
        .build());
  }

  @Primary
  @Bean
  @Named(REST_OBJECT_MAPPER)
  public ObjectMapper nflowRestObjectMapper(@NFlow ObjectMapper nflowObjectMapper) {
    ObjectMapper restObjectMapper = nflowObjectMapper.copy();
    restObjectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    restObjectMapper.enable(FAIL_ON_TRAILING_TOKENS);
    return restObjectMapper;
  }

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
