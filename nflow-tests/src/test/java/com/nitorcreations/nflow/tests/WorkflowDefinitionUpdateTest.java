package com.nitorcreations.nflow.tests;

import static com.nitorcreations.nflow.tests.demo.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static com.nitorcreations.nflow.tests.demo.StateWorkflow.STATE_WORKFLOW_TYPE;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.tests.demo.DemoWorkflow;
import com.nitorcreations.nflow.tests.demo.StateWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class WorkflowDefinitionUpdateTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().springContextClass(FirstConfiguration.class)
      .prop("nflow.db.h2.url", "jdbc:h2:mem:workflowdefinitionupdatetest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1").build();

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
    public StateWorkflow stateWorkflow() {
      return new StateWorkflow();
    }
  }

  @Test
  public void t01_demoWorkflowDefinitionIsReturned() {
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(DEMO_WORKFLOW_TYPE, definitions, true);
    assertWorkflowDefinitionExists(STATE_WORKFLOW_TYPE, definitions, false);
  }

  @Test
  public void t02_stopServer() {
    // This does not actually stop the executor threads, because JVM does not
    // exit.
    // Connection pool is closed though, so the workflow instance state cannot
    // be updated by the stopped nflow engine.
    server.stopServer();
  }

  @Test
  public void t03_restartServerWithDifferentConfiguration() throws Exception {
    server.setSpringContextClass(SecondConfiguration.class);
    server.startServer();
  }

  @Test
  public void t04_bothDefinitionsAreReturned() {
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(DEMO_WORKFLOW_TYPE, definitions, true);
    assertWorkflowDefinitionExists(STATE_WORKFLOW_TYPE, definitions, true);
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
