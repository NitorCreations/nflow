package io.nflow.tests;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.config.PropertiesConfiguration;
import io.nflow.tests.config.RestClientConfiguration;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.SkipTestMethodsAfterFirstFailureExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith({ SpringExtension.class, SkipTestMethodsAfterFirstFailureExtension.class })
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public class ConcurrentEnginesTest {
  private static final int ENGINES = 4;
  private static final int WORKFLOWS = 100 * ENGINES;
  private static final String UNIQUE_KEY = UUID.randomUUID().toString();
  private static final List<NflowServerConfig> servers = new ArrayList<>();
  private WebClient workflowInstanceResource;

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @BeforeAll
  public static void start() throws Exception {
    servers.add(new NflowServerConfig.Builder()
        .prop("nflow.executor.thread.count", 2)
        .prop("nflow.dispatcher.executor.queue.size", 5)
        .prop("nflow.executor.group", "concur")
        .prop("nflow.dispatcher.sleep.ms", 5)
        .prop("nflow.dispatcher.sleep.ms", 2)
        .springContextClass(DemoConfiguration.class).build());
    servers.get(0).before("ConcurrentEnginesTest");
    for (int i = 1; i < ENGINES; ++i) {
      NflowServerConfig server = servers.get(0).anotherServer();
      servers.add(server);
      server.before("ConcurrentEnginesTest");
    }
  }

  @Inject
  public void setWorkflowInstanceResource(@Named("workflowInstance") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(servers.get(0).getPort()).build().toString();
    this.workflowInstanceResource = fromClient(client, true).to(newUri, false);
  }

  @AfterAll
  public static void stop() {
    Collections.reverse(servers);
    servers.forEach(NflowServerConfig::after);
  }

  @Test
  @Order(1)
  public void insertWorkflows() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = DEMO_WORKFLOW_TYPE;
    req.businessKey = UNIQUE_KEY;
    req.activationTime = now().plusSeconds(10);
    for (int i = 0; i < WORKFLOWS; ++i) {
      CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req,
          CreateWorkflowInstanceResponse.class);
      assertThat(resp.id, notNullValue());
    }
  }

  @Test
  @Order(2)
  public void waitWorkflowsReady() {
    ListWorkflowInstanceResponse[] wfr = assertTimeoutPreemptively(ofSeconds(60), () -> {
      while (true) {
        sleep(500);
        ListWorkflowInstanceResponse[] instances = fromClient(workflowInstanceResource, true)
            .query("type", DEMO_WORKFLOW_TYPE)
            .query("maxResults", WORKFLOWS + 1)
            .query("businessKey", UNIQUE_KEY)
            .query("status", finished)
            .get(ListWorkflowInstanceResponse[].class);
        if (instances.length == WORKFLOWS) {
          return instances;
        }
      }
    });
    assertThat(wfr.length, is(WORKFLOWS));
  }

}
