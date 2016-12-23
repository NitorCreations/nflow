package io.nflow.engine.internal.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesConfigurationTest {

  @SuppressWarnings("unused")
  private PropertiesConfiguration configuration;

  @Test
  public void propertiesConfigurationInstantiated() {
    configuration = new PropertiesConfiguration();
  }

}
