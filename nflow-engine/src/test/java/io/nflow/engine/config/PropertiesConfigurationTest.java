package io.nflow.engine.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.config.PropertiesConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesConfigurationTest {

  @SuppressWarnings("unused")
  private PropertiesConfiguration configuration;

  @Test
  public void propertiesConfigurationInstantiated() {
    configuration = new PropertiesConfiguration();
  }

}
