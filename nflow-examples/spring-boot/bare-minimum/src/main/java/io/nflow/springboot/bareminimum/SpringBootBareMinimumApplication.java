package io.nflow.springboot.bareminimum;

import javax.inject.Inject;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@SpringBootApplication
@Import(EngineConfiguration.class)
public class SpringBootBareMinimumApplication {

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
  }

  @Bean
  public ApplicationRunner applicationRunner() {
    return new ExampleApplicationRunner();
  }

  public static void main(String[] args) {
    SpringApplication.run(SpringBootBareMinimumApplication.class, args);
  }

  public static class ExampleApplicationRunner implements ApplicationRunner {

    @Inject
    private WorkflowInstanceService workflowInstances;

    @Inject
    private WorkflowInstanceFactory workflowInstanceFactory;

    @Override
    public void run(ApplicationArguments args) {
      workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
          .setType(ExampleWorkflow.TYPE)
          .setExternalId("example")
          .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
          .build());
    }
  }
}
