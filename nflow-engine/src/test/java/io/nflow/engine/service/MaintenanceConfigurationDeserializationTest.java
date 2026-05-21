package io.nflow.engine.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.Period.days;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;

class MaintenanceConfigurationDeserializationTest {

  private final ObjectMapper mapper;

  MaintenanceConfigurationDeserializationTest() {
    mapper = new ObjectMapper();
    mapper.setDefaultPropertyInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
  }

  @Test
  void maintenanceConfigurationRoundTrip() throws Exception {
    MaintenanceConfiguration original = new MaintenanceConfiguration.Builder()
        .withArchiveWorkflows().setOlderThanPeriod(days(30)).setBatchSize(500).done()
        .withDeleteArchivedWorkflows().setOlderThanPeriod(days(90)).setBatchSize(100).done()
        .withDeleteExpiredExecutorsOlderThan(days(7))
        .build();

    String json = mapper.writeValueAsString(original);
    MaintenanceConfiguration deserialized = mapper.readValue(json, MaintenanceConfiguration.class);

    assertThat(deserialized.archiveWorkflows, notNullValue());
    assertThat(deserialized.archiveWorkflows.batchSize, is(original.archiveWorkflows.batchSize));
    assertThat(deserialized.deleteArchivedWorkflows, notNullValue());
    assertThat(deserialized.deleteArchivedWorkflows.batchSize, is(original.deleteArchivedWorkflows.batchSize));
  }

  @Test
  void configurationItemRoundTrip() throws Exception {
    ConfigurationItem original = new MaintenanceConfiguration.Builder()
        .withArchiveWorkflows().setOlderThanPeriod(days(30)).setBatchSize(200).done()
        .build()
        .archiveWorkflows;

    String json = mapper.writeValueAsString(original);
    ConfigurationItem deserialized = mapper.readValue(json, ConfigurationItem.class);

    assertThat(deserialized.batchSize, is(original.batchSize));
  }
}
