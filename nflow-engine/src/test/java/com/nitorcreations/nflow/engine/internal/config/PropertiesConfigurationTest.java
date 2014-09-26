package com.nitorcreations.nflow.engine.internal.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesConfigurationTest {

  @SuppressWarnings("unused")
  private PropertiesConfiguration configuration;

  @Test
  public void propertiesConfigurationInstantiated() {
    configuration = new PropertiesConfiguration();
  }

}
