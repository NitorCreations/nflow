package com.nitorcreations.nflow.jetty.spring;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

public class NflowStandardEnvironment extends StandardEnvironment {
  @SuppressWarnings("hiding")
  private static final Logger logger = getLogger(NflowStandardEnvironment.class);

  public NflowStandardEnvironment(Map<String, Object> overrideProperties) {
    getPropertySources().addLast(new MapPropertySource("override", overrideProperties));
    String env = getProperty("env", "local");
    addActiveProfile(env);
    addPropertyResource(env);
    addPropertyResource("common");
    addPropertyResource("nflow-jetty");
    String profiles = getProperty("profiles", String.class, "");
    for (String profile : profiles.split(",")) {
      if (!profile.trim().isEmpty()) {
        addActiveProfile(profile);
      }
    }
    setupDbProfile();
  }

  private void setupDbProfile() {
    boolean dbProfileDefined = false;
    for (String profile : asList(getActiveProfiles())) {
      if (profile.startsWith("nflow.db")) {
        if (dbProfileDefined) {
          throw new RuntimeException("Multiple nflow.db-profiles defined");
        }
        dbProfileDefined = true;
      }
    }
    if (!dbProfileDefined) {
      addActiveProfile("nflow.db.h2");
    }
  }

  private void addPropertyResource(String name) {
    name += ".properties";
    try {
      getPropertySources().addLast(new ResourcePropertySource(name, getClass().getClassLoader()));
    } catch (IOException e) {
      logger.info("Failed to initialize environment-specific properties from resource {}", name);
    }
  }
}
