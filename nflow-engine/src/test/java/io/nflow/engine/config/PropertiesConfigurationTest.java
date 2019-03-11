package io.nflow.engine.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PropertiesConfigurationTest {

  @SuppressWarnings("unused")
  private PropertiesConfiguration configuration;

  @Test
  public void propertiesConfigurationInstantiated() {
    configuration = new PropertiesConfiguration();
  }

}
