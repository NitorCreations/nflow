package io.nflow.rest.v1.config.jaxrs;

import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import io.nflow.rest.config.RestConfiguration;

@ExtendWith(MockitoExtension.class)
public class RestConfigurationTest {

  RestConfiguration configuration;

  @BeforeEach
  public void setup() {
    configuration = new RestConfiguration();
  }

  @Test
  public void nflowRestObjectMapperInstantiated() {
    JsonMapper restMapper = configuration.nflowRestObjectMapper(JsonMapper::new);
    assertThat(restMapper.serializationConfig().isEnabled(WRITE_DATES_AS_TIMESTAMPS), is(false));
  }
}
