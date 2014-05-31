package com.nitorcreations.nflow.jetty.spring;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

public class NflowStandardEnvironment extends StandardEnvironment {
  private static final Logger LOG = getLogger(NflowStandardEnvironment.class);

  public NflowStandardEnvironment(Map<String, Object> overrideProperties) {
    getPropertySources().addLast(new MapPropertySource("override", overrideProperties));
    addPropertyResource(getProperty("env", "local"));
    addPropertyResource("common");
    String profiles = getProperty("profiles", String.class, "");
    for (String profile : profiles.split(",")) {
      if (!profile.trim().isEmpty()) {
        addActiveProfile(profile);
      }
    }
  }

  private void addPropertyResource(String name) {
    name += ".properties";
    try {
      getPropertySources().addLast(new ResourcePropertySource(name, getClass().getClassLoader()));
    } catch (IOException e) {
      LOG.info("Failed to initialize environment-specific properties from resource {}", name);
    }
  }
}
