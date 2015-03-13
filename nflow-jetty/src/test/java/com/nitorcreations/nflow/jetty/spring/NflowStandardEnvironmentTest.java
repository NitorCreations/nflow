package com.nitorcreations.nflow.jetty.spring;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.core.env.AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NflowStandardEnvironmentTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    setProperty("env", "junit");
    setProperty(IGNORE_GETENV_PROPERTY_NAME, "true");
  }

  @After
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
  public void profilesPropertyEnablesSpringProfiles() {
    setProperty("profiles", "plain.test.profile,other.test.profile");
    NflowStandardEnvironment environment = new NflowStandardEnvironment(new HashMap<String, Object>());
    assertThat(environment.acceptsProfiles("plain.test.profile", "other.test.profile"), is(true));
  }

  @Test
  public void multipleDatabaseProfilesPrevented() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Multiple nflow.db-profiles defined");
    setProperty("profiles", "nflow.db.profile1,nflow.db.profile2");
    new NflowStandardEnvironment(new HashMap<String, Object>());
  }

  @Test
  public void databaseProfileDefaultsToH2() {
    NflowStandardEnvironment environment = new NflowStandardEnvironment(new HashMap<String, Object>());
    assertThat(environment.acceptsProfiles("nflow.db.h2"), is(true));
  }
}
