package io.nflow.server.spring;

import static io.nflow.engine.config.Profiles.H2;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Arrays;
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
    addExternalPropertyResource();
    String env = getProperty("env", "local");
    addActiveProfile(env);
    addPropertyResource(env);
    addPropertyResource("common");
    addPropertyResource("nflow-server");
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
    String[] activeProfiles = getActiveProfiles();
    for (String profile : activeProfiles) {
      if (profile.startsWith("nflow.db")) {
        if (dbProfileDefined) {
          throw new RuntimeException("Multiple nflow.db profiles defined: " + Arrays.toString(activeProfiles));
        }
        dbProfileDefined = true;
      }
    }
    if (!dbProfileDefined) {
      addActiveProfile(H2);
    }
  }

  private void addExternalPropertyResource() {
    String externalLocation = getProperty("nflow.external.config");
    if (!isEmpty(externalLocation)) {
      try {
        getPropertySources().addLast(new ResourcePropertySource(externalLocation));
        logger.info("Using external configuration file: {}", externalLocation);
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize external properties from location " + externalLocation, e);
      }
    }
  }

  private void addPropertyResource(String name) {
    name += ".properties";
    try {
      getPropertySources().addLast(new ResourcePropertySource(name, getClass().getClassLoader()));
    } catch (@SuppressWarnings("unused") IOException e) {
      logger.info("Failed to initialize environment-specific properties from resource {}", name);
    }
  }
}
