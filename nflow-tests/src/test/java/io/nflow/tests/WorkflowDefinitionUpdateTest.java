package io.nflow.tests;

import static io.nflow.tests.demo.workflow.Demo2Workflow.DEMO2_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static org.junit.jupiter.api.Assertions.fail;

import io.nflow.tests.demo.workflow.Demo2Workflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;

import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.nflow.tests.demo.workflow.DemoWorkflow;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WorkflowDefinitionUpdateTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
          .springContextClass(FirstConfiguration.class)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:workflowdefinitionupdatetest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
          .build();

  public WorkflowDefinitionUpdateTest() {
    super(server);
  }

  static class FirstConfiguration {
    @Bean
    public DemoWorkflow demoWorkflow() {
      return new DemoWorkflow();
    }
  }

  static class SecondConfiguration {
    @Bean
    public Demo2Workflow demo2Workflow() {
      return new Demo2Workflow();
    }
  }

  @Test
  @Order(1)
  public void demoWorkflowDefinitionIsReturned() {
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(DEMO_WORKFLOW_TYPE, definitions, true);
    assertWorkflowDefinitionExists(DEMO2_WORKFLOW_TYPE, definitions, false);
  }

  @Test
  @Order(2)
  public void stopServer() {
    // This does not actually stop the executor threads, because JVM does not
    // exit.
    // Connection pool is closed though, so the workflow instance state cannot
    // be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  @Order(3)
  public void restartServerWithDifferentConfiguration() throws Exception {
    server.setSpringContextClass(SecondConfiguration.class);
    server.startServer();
  }

  @Test
  @Order(4)
  public void bothDefinitionsAreReturned() {
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(DEMO_WORKFLOW_TYPE, definitions, true);
    assertWorkflowDefinitionExists(DEMO2_WORKFLOW_TYPE, definitions, true);
  }

  private void assertWorkflowDefinitionExists(String type, ListWorkflowDefinitionResponse[] definitions, boolean shouldExist) {
    for (ListWorkflowDefinitionResponse def : definitions) {
      if (type.equals(def.type)) {
        if (shouldExist) {
          return;
        }
        fail("Workflow definition " + type + " should not be found");
      }
    }
    if (shouldExist) {
      fail("Workflow definition " + type + " is missing");
    }
  }
}
