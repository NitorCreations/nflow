package io.nflow.rest.v1.config.jaxrs;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    ObjectMapper restMapper = configuration.nflowRestObjectMapper(new ObjectMapper());
    assertThat(restMapper.getSerializationConfig().hasSerializationFeatures(WRITE_DATES_AS_TIMESTAMPS.getMask()), is(false));
  }
}
