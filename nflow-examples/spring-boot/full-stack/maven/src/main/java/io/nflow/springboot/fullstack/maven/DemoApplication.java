package io.nflow.springboot.fullstack.maven;

import javax.inject.Inject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@SpringBootApplication
@Import(EngineConfiguration.class)
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

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
