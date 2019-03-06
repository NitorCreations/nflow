package io.nflow.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MetricsAdminServletTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:executorrecoverytest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
          .build();

  public MetricsAdminServletTest() {
    super(server);
  }

  @Test
  @Order(1)
  public void canFetchMetrics() {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/nflow/metrics/metrics");
    makeRequest(uri);
  }

  @Test
  @Order(2)
  public void canFetchHealthChecks() {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/nflow/metrics/healthcheck");
    makeRequest(uri);
  }

  private void makeRequest(URI uri) {
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(uri);
    try (Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get()) {
      assertEquals(200, response.getStatus());
    }
  }
}
