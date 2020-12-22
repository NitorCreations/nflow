package io.nflow.netty;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_DEFINITION_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

public class StartNflowTest {

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
    assertEquals("7500", ctx.getEnvironment().getProperty("port"));
    assertEquals("local", ctx.getEnvironment().getProperty("env"));
    assertEquals("externallyDefinedExecutorGroup", ctx.getEnvironment().getProperty("nflow.executor.group"));
    assertEquals("true", ctx.getEnvironment().getProperty("nflow.db.create_on_startup"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autostart"));
    assertEquals("true", ctx.getEnvironment().getProperty("nflow.autoinit"));

    // Smoke test here also the netty REST API by fetching workflow definitions
    WebClient client = WebClient.builder().baseUrl("http://localhost:7500")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    ClientResponse response = client.get().uri(restApiPrefix + NFLOW_WORKFLOW_DEFINITION_PATH).exchange().block();
    assertEquals(HttpStatus.OK, response.statusCode());
    JsonNode responseBody = response.bodyToMono(JsonNode.class).block();
    assertTrue(responseBody.isArray());
  }

}
