package io.nflow.tests;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.nflow.tests.demo.workflow.Demo2Workflow;
import io.nflow.tests.extension.NflowServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.nflow.tests.demo.workflow.RemoteWorkflow.REMOTE_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.TestState.*;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CodelessEngineTest extends AbstractNflowTest {

  public static final NflowServerConfig server = new NflowServerConfig.Builder()
          .springContextClass(EmptyConfiguration.class)
          .prop("nflow.autoinit", "false")
          .prop("nflow.autostart", "false")
          .prop("nflow.db.create_on_startup", "true")
          .prop("nflow.definition.refreshStoredFromDatabase.interval.seconds", "1")
          .prop("nflow.maintenance.insertWorkflowIfMissing", "false")
          .prop("nflow.db.h2.url", "jdbc:h2:mem:codelessenginetest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
          .build();

  public static final AtomicReference<NflowServerConfig> codeServer = new AtomicReference<>();
  private static CreateWorkflowInstanceResponse resp;

  public CodelessEngineTest() {
    super(server);
  }

  static class EmptyConfiguration {
  }

  static class CodeConfiguration {
    @Bean
    public Demo2Workflow demo2Workflow() {
      return new Demo2Workflow();
    }
  }

  @Test
  @Order(1)
  public void codelessServerHasNoWorkflowsDefinitions() {
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(REMOTE_WORKFLOW_TYPE, definitions, false);
  }

  @Test
  @Order(2)
  public void startCodeServer() throws Exception {
    server.setSpringContextClass(CodeConfiguration.class);
    codeServer.set(server.anotherServer(Map.of(
            "nflow.autoinit", "true",
            "nflow.autostart", "true",
            "nflow.non_spring_workflows_filename", "nflow-remote-workflow.txt")));
    codeServer.get().before(getClass().getSimpleName());
  }

  @Test
  @Order(3)
  public void codelessServerSeesTheDefinition() throws Exception {
    SECONDS.sleep(1);
    ListWorkflowDefinitionResponse[] definitions = getWorkflowDefinitions();
    assertWorkflowDefinitionExists(REMOTE_WORKFLOW_TYPE, definitions, true);
  }

  @Test
  @Order(4)
  public void codelessCanInsertWorkflow() {
    var req = new CreateWorkflowInstanceRequest();
    req.type = REMOTE_WORKFLOW_TYPE;
    req.businessKey = "1";
    req.stateVariables = singletonMap("test", 1);
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(5)
  public void pollWorkflowToComplete() {
    assertThat(getWorkflowInstanceWithTimeout(resp.id, DONE.name(), ofSeconds(30)), notNullValue());
  }

  @AfterAll
  public static void shutdown() {
    codeServer.get().after();
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
