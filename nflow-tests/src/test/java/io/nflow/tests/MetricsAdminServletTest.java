package io.nflow.tests;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MetricsAdminServletTest extends AbstractNflowTest {

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
  public void canFetchMetrics() {
    var metrics = getMetricsStatistics();
    assertThat(metrics.get("gauges").get("threadStates.deadlock.count").get("value").asInt(), is(0));
  }

  @Test
  public void canFetchHealthChecks() {
    var health = getMetricsHealth();
    assertThat(health.get("nflowDatabaseConnection").get("healthy").asBoolean(), is(true));
  }

  @Test
  public void metricsHasDatabaseTypeExposed() {
    var metrics = getMetricsStatistics();
    var springProfile = getenv("SPRING_PROFILES_ACTIVE");
    var dbType = metrics.get("gauges").get("nflow.database.type").get("value").asText();
    System.out.printf("Database type %s, profile %s%n", dbType, springProfile);

    var springProfiles = ofNullable(springProfile).map(s -> s.split(",")).orElse(new String[0]);
    var profileDbType = Stream.of(springProfiles)
            .filter(p -> p.startsWith("nflow.db."))
            .map(p -> p.substring(9))
            .findFirst()
            .orElse("h2");
    assertThat(dbType, is(profileDbType));
  }
}
