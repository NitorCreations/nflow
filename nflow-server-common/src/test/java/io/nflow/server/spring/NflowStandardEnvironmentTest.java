package io.nflow.server.spring;

import static io.nflow.engine.config.Profiles.H2;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.core.env.AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Profiles;

@ExtendWith(MockitoExtension.class)
public class NflowStandardEnvironmentTest {

  @BeforeEach
  public void setup() {
    setProperty("env", "junit");
    setProperty(IGNORE_GETENV_PROPERTY_NAME, "true");
  }

  @AfterEach
  public void cleanup() {
    clearProperty("env");
    clearProperty("profiles");
    clearProperty(IGNORE_GETENV_PROPERTY_NAME);
  }

  @Test
  public void environmentSpecificPropertiesEffective() {
    NflowStandardEnvironment environment = new NflowStandardEnvironment(new HashMap<String, Object>());
    assertThat(environment.getProperty("nflow.executor.group"), is("junit"));
  }

  @Test
  public void overridePropertiesEffective() {
    Map<String, Object> overrideProperties = new HashMap<>();
    overrideProperties.put("nflow.executor.group", "overriddenExecutorGroup");
    NflowStandardEnvironment environment = new NflowStandardEnvironment(overrideProperties);
    assertThat(environment.getProperty("nflow.executor.group"), is("overriddenExecutorGroup"));
  }

  @Test
  public void externalPropertiesEffective() {
    Map<String, Object> overrideProperties = new HashMap<>();
    overrideProperties.put("nflow.external.config", "/external.properties");
    NflowStandardEnvironment environment = new NflowStandardEnvironment(overrideProperties);
    assertThat(environment.getProperty("nflow.executor.group"), is("externallyDefinedExecutorGroup"));
  }

  @Test
  public void missingExternalPropertiesException() {
    Map<String, Object> overrideProperties = new HashMap<>();
    overrideProperties.put("nflow.external.config", "/missing.properties");
    assertThrows(RuntimeException.class, () -> new NflowStandardEnvironment(overrideProperties));
  }

  @Test
  public void profilesPropertyEnablesSpringProfiles() {
    setProperty("profiles", "plain.test.profile,other.test.profile");
    NflowStandardEnvironment environment = new NflowStandardEnvironment(new HashMap<String, Object>());
    assertThat(environment.acceptsProfiles(Profiles.of("plain.test.profile", "other.test.profile")), is(true));
  }

  @Test
  public void multipleDatabaseProfilesPrevented() {
    setProperty("profiles", "nflow.db.profile1,nflow.db.profile2");

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> new NflowStandardEnvironment(new HashMap<String, Object>()));
    assertThat(thrown.getMessage(), CoreMatchers.containsString("Multiple nflow.db profiles defined"));
  }

  @Test
  public void databaseProfileDefaultsToH2() {
    NflowStandardEnvironment environment = new NflowStandardEnvironment(new HashMap<String, Object>());
    assertThat(environment.acceptsProfiles(Profiles.of(H2)), is(true));
  }
}
