package io.nflow.engine.internal.guice;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;
import org.springframework.core.env.Environment;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EngineEnvironmentModuleTest {

  @Test
  public void testEngineEnvironmentModuleWithDeafultValues() {
    Injector injector = Guice.createInjector(new EngineEnvironmentModule(null));
    Environment env = injector.getInstance(Environment.class);
    assertThat(env.getProperty("nflow.autostart"), is("true"));
    assertThat(env.getProperty("nflow.executor.group"), is("nflow"));
    assertThat(env.getProperty("nflow.executor.timeout.seconds"), is("900"));
  }

  @Test
  public void testEngineEnvironmentModuleWithCustomizedProperties() {
    Properties p = new Properties();
    p.put("nflow.autostart", "false");
    p.put("nflow.executor.timeout.seconds", "800");
    Injector injector = Guice.createInjector(new EngineEnvironmentModule(p));
    Environment env = injector.getInstance(Environment.class);
    assertThat(env.getProperty("nflow.autostart"), is("false"));
    assertThat(env.getProperty("nflow.executor.group"), is("nflow"));
    assertThat(env.getProperty("nflow.executor.timeout.seconds"), is("800"));
  }

  @Test
  public void testEngineEnvironmentModuleWithClasspathFile() {
    Injector injector = Guice.createInjector(new EngineEnvironmentModule(null, "nflow-engine-test.properties"));
    Environment env = injector.getInstance(Environment.class);
    assertThat(env.getProperty("nflow.autostart"), is("false"));
    assertThat(env.getProperty("nflow.executor.group"), is("nflow"));
    assertThat(env.getProperty("nflow.executor.timeout.seconds"), is("800"));
  }
}
