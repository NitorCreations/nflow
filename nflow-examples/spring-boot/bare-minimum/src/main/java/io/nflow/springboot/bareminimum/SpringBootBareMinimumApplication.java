package io.nflow.springboot.bareminimum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.nflow.engine.internal.config.EngineConfiguration;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@SpringBootApplication
@Import(EngineConfiguration.class)
@ComponentScan("io.nflow.springbootbareminimum")
public class SpringBootBareMinimumApplication {

  @Inject
  private WorkflowInstanceService workflowInstances;

  @Inject
  private WorkflowInstanceFactory workflowInstanceFactory;

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
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
    SpringApplication.run(SpringBootBareMinimumApplication.class, args);
  }
}
