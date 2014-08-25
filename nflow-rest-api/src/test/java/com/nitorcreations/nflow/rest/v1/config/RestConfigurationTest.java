package com.nitorcreations.nflow.rest.v1.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.rest.config.RestConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class RestConfigurationTest {

  @SuppressWarnings("unused")
  private RestConfiguration configuration;

  @Test
  public void restConfigurationInstantiated() {
    configuration = new RestConfiguration();
  }

}
