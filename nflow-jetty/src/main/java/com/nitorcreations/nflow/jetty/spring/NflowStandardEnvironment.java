package com.nitorcreations.nflow.jetty.spring;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

public class NflowStandardEnvironment extends StandardEnvironment {

  private static final Logger LOG = getLogger(NflowStandardEnvironment.class);

  @Override
  protected void customizePropertySources(MutablePropertySources propertySources) {
    super.customizePropertySources(propertySources);
    try {
      propertySources.addLast(new ResourcePropertySource(System.getProperty("env", "dev") + ".properties",
          NflowStandardEnvironment.class.getClassLoader()));
    } catch (IOException e) {
      LOG.warn("Failed to initialize environment-specific properties", e);
    }
  }

}
