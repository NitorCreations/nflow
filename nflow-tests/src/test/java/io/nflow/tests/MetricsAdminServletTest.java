package io.nflow.tests;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MetricsAdminServletTest extends AbstractNflowTest {
  @Inject
  @Named("workflowInstance")
  private WebClient baseClient;

  public static NflowServerConfig server = new NflowServerConfig.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .metrics(true)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:executorrecoverytest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
          .build();

  public MetricsAdminServletTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void canFetchMetrics() {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/nflow/metrics/metrics");
    var metrics = makeRequest(uri);
    var dbType = metrics.get("gauges").get("nflow.database.type").get("value").asText();
    var springProfiles = ofNullable(getenv("SPRING_PROFILES_ACTIVE")).map(s -> s.split(",")).orElse(new String[0]);
    var profileDbType = Stream.of(springProfiles)
            .filter(p -> p.startsWith("nflow.db."))
            .map(p -> p.substring(9))
            .findFirst()
            .orElse("h2");
    assertThat(dbType, is(profileDbType));
  }

  @Test
  @Order(2)
  public void canFetchHealthChecks() {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/nflow/metrics/healthcheck");
    makeRequest(uri);
  }

  private JsonNode makeRequest(URI uri) {
    var client = fromClient(baseClient, true).to(uri.toString(), false);
    return client.get(JsonNode.class);
  }
}
