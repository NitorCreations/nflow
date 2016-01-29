package com.nitorcreations.nflow.tests;

import com.nitorcreations.nflow.tests.runner.NflowServerRule;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
public class MetricsAdminServletTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder()
          .prop("nflow.executor.timeout.seconds", 1)
          .prop("nflow.executor.keepalive.seconds", 5)
          .prop("nflow.dispatcher.await.termination.seconds", 1)
          .prop("nflow.db.h2.url", "jdbc:h2:mem:executorrecoverytest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
          .build();

  public MetricsAdminServletTest() {
    super(server);
  }

  @Test
  public void t01_canFetchMetrics() throws IOException {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/metrics/metrics");
    makeRequest(uri);
  }

  @Test
  public void t02_canFetchHealthChecks() throws IOException {
    URI uri = URI.create("http://localhost:" + server.getPort() + "/metrics/healthcheck");
    makeRequest(uri);
  }

  private void makeRequest(URI uri) throws IOException {
    try(CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(uri);
      HttpResponse response = httpclient.execute(request);
      assertEquals(200, response.getStatusLine().getStatusCode());
    }
  }
}
