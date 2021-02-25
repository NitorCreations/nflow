package io.nflow.netty;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_DEFINITION_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_INSTANCE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

public class StartNflowTest {

  private static final String DEFAULT_LOCALHOST_SERVER_PORT = "7500";
  private static final String DEFAULT_LOCALHOST_SERVER_ADDRESS = "http://localhost:" + DEFAULT_LOCALHOST_SERVER_PORT;

  public class TestApplicationListener implements ApplicationListener<ApplicationContextEvent> {
    public ApplicationContextEvent applicationContextEvent;

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
      applicationContextEvent = event;
    }
  }

  @Test
  public void startNflowNetty() throws Exception {
    TestApplicationListener testListener = new TestApplicationListener();
    StartNflow startNflow = new StartNflow().registerSpringContext(this.getClass())
        .registerSpringClasspathPropertySource("external.properties")
        .registerSpringApplicationListener(testListener);
    Map<String, Object> properties = new HashMap<>();
    String restApiPrefix = "nflow/api";
    properties.put("nflow.db.create_on_startup", true);
    properties.put("nflow.autostart", false);
    properties.put("nflow.autoinit", true);
    properties.put("nflow.rest.path.prefix", restApiPrefix);

    ApplicationContext ctx = startNflow.startNetty(7500, "local", "", properties);

    assertNotNull(testListener.applicationContextEvent);
    assertEquals(DEFAULT_LOCALHOST_SERVER_PORT, ctx.getEnvironment().getProperty("port"));
    assertEquals("local", ctx.getEnvironment().getProperty("env"));
    assertEquals("externallyDefinedExecutorGroup", ctx.getEnvironment().getProperty("nflow.executor.group"));
    assertEquals("true", ctx.getEnvironment().getProperty("nflow.db.create_on_startup"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autostart"));
    assertEquals("true", ctx.getEnvironment().getProperty("nflow.autoinit"));

    smokeTestRestApi(restApiPrefix);
    smokeTestRestApiErrorHandling(restApiPrefix);
  }

  private void smokeTestRestApi(String restApiPrefix) {
    ClientResponse response = getFromDefaultServer(restApiPrefix + NFLOW_WORKFLOW_DEFINITION_PATH);
    assertEquals(OK, response.statusCode());
    JsonNode responseBody = response.bodyToMono(JsonNode.class).block();
    assertTrue(responseBody.isArray());
  }

  // Smoke test for io.nflow.rest.v1.springweb.SpringWebResource#handleExceptions
  private void smokeTestRestApiErrorHandling(String restApiPrefix) {
    ClientResponse response = getFromDefaultServer(restApiPrefix + NFLOW_WORKFLOW_INSTANCE_PATH + "/id/0213132");
    assertEquals(NOT_FOUND, response.statusCode());
    JsonNode responseBody = response.bodyToMono(JsonNode.class).block();
    assertNotNull(responseBody);
    assertFalse(responseBody.isEmpty());
  }

  private ClientResponse getFromDefaultServer(String url) {
    WebClient client = WebClient.builder().baseUrl(DEFAULT_LOCALHOST_SERVER_ADDRESS).build();
    return client.get().uri(url).exchange().block();
  }
}
