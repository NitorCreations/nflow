package io.nflow.springboot.fullstack;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.DateTimeParamConverterProvider;
import io.nflow.rest.config.RestConfiguration;
import io.nflow.rest.v1.ArchiveResource;
import io.nflow.rest.v1.StatisticsResource;
import io.nflow.rest.v1.WorkflowDefinitionResource;
import io.nflow.rest.v1.WorkflowExecutorResource;
import io.nflow.rest.v1.WorkflowInstanceResource;

@SpringBootApplication
@Import(RestConfiguration.class)
public class SpringBootFullStackApplication {

  @Inject
  private WorkflowInstanceService workflowInstances;

  @Inject
  private WorkflowInstanceFactory workflowInstanceFactory;

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
  }

  @Bean
  @Primary
  public ObjectMapper springWebObjectMapper(@NFlow ObjectMapper nflowObjectMapper) {
    return nflowObjectMapper;
  }

  @Bean
  public JerseyResourceConfig jerseyResourceConfig() {
    return new JerseyResourceConfig();
  }

  private static class JerseyResourceConfig extends ResourceConfig {
    public JerseyResourceConfig() {
      register(ArchiveResource.class);
      register(WorkflowDefinitionResource.class);
      register(WorkflowExecutorResource.class);
      register(WorkflowInstanceResource.class);
      register(StatisticsResource.class);
      register(DateTimeParamConverterProvider.class);
      register(JacksonFeature.class);
    }
  }

  @PostConstruct
  public void createExampleWorkflowInstance() {
    workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
            .setType(ExampleWorkflow.TYPE)
            .setExternalId("example")
            .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
            .build());
  }

  public static void main(String[] args) {
    SpringApplication.run(SpringBootFullStackApplication.class, args);
  }
}
