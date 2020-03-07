package io.nflow.springboot.fullstack;

import javax.inject.Inject;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.RestConfiguration;
import io.nflow.rest.config.jaxrs.DateTimeParamConverterProvider;
import io.nflow.rest.v1.jaxrs.ArchiveResource;
import io.nflow.rest.v1.jaxrs.StatisticsResource;
import io.nflow.rest.v1.jaxrs.WorkflowDefinitionResource;
import io.nflow.rest.v1.jaxrs.WorkflowExecutorResource;
import io.nflow.rest.v1.jaxrs.WorkflowInstanceResource;

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

  public static void main(String[] args) {
    SpringApplication.run(SpringBootFullStackApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void insertExampleWorkflow() {
    workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
        .setType(ExampleWorkflow.TYPE)
        .setExternalId("example")
        .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
        .build());
  }
}
