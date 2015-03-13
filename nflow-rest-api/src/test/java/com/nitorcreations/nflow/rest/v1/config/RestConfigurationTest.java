package com.nitorcreations.nflow.rest.v1.config;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.rest.config.RestConfiguration;

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
    assertThat(restMapper.getSerializationConfig().hasMapperFeatures(WRITE_DATES_AS_TIMESTAMPS.getMask()), is(true));
  }
}
