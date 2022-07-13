package io.nflow.tests;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static io.nflow.tests.AbstractNflowTest.nflowObjectMapper;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.FIBONACCI_TYPE;
import static io.nflow.tests.demo.workflow.FibonacciWorkflow.VAR_REQUEST_DATA;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyMap;
import static java.util.Collections.reverse;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import io.nflow.tests.demo.workflow.FibonacciWorkflow;
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
  private static final Set<Long> workflowIds = new HashSet<>();
  private WebClient workflowInstanceResource;

  @Inject
  @Named("workflowInstance")
  private WebClient baseClient;

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
        .metrics(true)
        .springContextClass(DemoConfiguration.class).build());
    servers.get(0).before("ConcurrentEnginesTest");
    for (int i = 1; i < ENGINES; ++i) {
      var server = servers.get(0).anotherServer(emptyMap());
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
    reverse(servers);
    servers.forEach(NflowServerConfig::after);
  }

  @Test
  @Order(1)
  public void insertWorkflows() {
    var req = new CreateWorkflowInstanceRequest();
    req.type = FIBONACCI_TYPE;
    req.businessKey = UNIQUE_KEY;
    req.activationTime = now().plusSeconds(5);
    req.stateVariables.put(VAR_REQUEST_DATA, nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(5)));

    for (int i = 0; i < WORKFLOWS; ++i) {
      var resp = workflowInstanceResource.put(req, CreateWorkflowInstanceResponse.class);
      assertThat(resp.id, notNullValue());
      workflowIds.add(resp.id);
    }
  }

  @Test
  @Order(2)
  public void waitWorkflowsReady() {
    var wfr = assertTimeoutPreemptively(ofSeconds(120), () -> {
      while (true) {
        sleep(500);
        var instances = workflowInstanceResource
            .reset()
            .query("type", FIBONACCI_TYPE)
            .query("maxResults", WORKFLOWS + 1)
            .query("businessKey", UNIQUE_KEY)
            .query("status", finished)
            .get(ListWorkflowInstanceResponse[].class);

        int count = WORKFLOWS;
        for (var workflow : instances) {
          if (workflowIds.contains(workflow.id)) {
            count--;
          } else {
            break;
          }
        }
        if (count == 0) {
          return instances;
        }
      }
    });
    assertThat(wfr.length, is(WORKFLOWS));
  }

  @Test
  @Order(3)
  public void verifyAllExecutorsExecutedWorkflows() {
    List<Integer> polls = new ArrayList<>();
    for (var server: servers) {
      var uri = URI.create("http://localhost:" + server.getPort() + "/nflow/metrics/metrics");
      var data = makeRequest(uri);
      var meters = data.get("meters");
      AtomicInteger count = new AtomicInteger();
      meters.fieldNames().forEachRemaining(field -> {
        if (field.matches("concur\\.[0-9]+\\.fibonacci\\.begin\\.success-count")) {
          count.set(meters.get(field).get("count").asInt());
        }
      });
      polls.add(count.get());
    }
    System.out.println(polls);
    assertThat("Each engine has to do at least 50% of its fair share of work", polls, everyItem(greaterThan(WORKFLOWS / ENGINES / 2)));
  }

  private JsonNode makeRequest(URI uri) {
    var client = fromClient(baseClient, true).to(uri.toString(), false);
    return client.get(JsonNode.class);
  }
}
