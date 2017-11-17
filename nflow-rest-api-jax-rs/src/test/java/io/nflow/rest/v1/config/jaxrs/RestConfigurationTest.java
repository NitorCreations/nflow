package io.nflow.rest.v1.config.jaxrs;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.rest.config.RestConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class RestConfigurationTest {

  RestConfiguration configuration;

  @Before
  public void setup() {
    configuration = new RestConfiguration();
  }

  @Test
  public void nflowRestObjectMapperInstantiated() {
    ObjectMapper restMapper = configuration.nflowRestObjectMapper(new ObjectMapper());
    assertThat(restMapper.getSerializationConfig().hasSerializationFeatures(WRITE_DATES_AS_TIMESTAMPS.getMask()), is(false));
  }
}
