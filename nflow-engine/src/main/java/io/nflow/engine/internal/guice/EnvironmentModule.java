package io.nflow.engine.internal.guice;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import com.google.inject.AbstractModule;

public class EnvironmentModule extends AbstractModule {

  private final Properties userProperties;
  private final String[] classpathPropertiesFiles;

  public EnvironmentModule(final Properties userProperties, String... classpathPropertiesFiles) {
    this.userProperties = userProperties;
    this.classpathPropertiesFiles = classpathPropertiesFiles;
  }

  protected void customizeEnvironment(MutablePropertySources propertySources) {
    propertySources.addFirst(new PropertiesPropertySource("nflowClasspathProperties", getClassPathConfigurationProperties()));
    propertySources.addFirst(new PropertiesPropertySource("nflowSystemProperties", System.getProperties()));
    if (userProperties != null) {
      propertySources.addFirst(new PropertiesPropertySource("nflowEngineUserProperties", userProperties));
    }
  }

  protected Properties getClassPathConfigurationProperties() {
    final Properties p = new Properties();
    if (classpathPropertiesFiles != null) {
      for (String classpathPropertiesFile : classpathPropertiesFiles) {
        ClassPathResource engineProperties = new ClassPathResource(classpathPropertiesFile);
        try {
          p.load(engineProperties.getInputStream());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return p;
  }

  @Override
  protected void configure() {
    bind(Environment.class).toInstance(new StandardEnvironment() {
      @Override
      protected void customizePropertySources(MutablePropertySources propertySources) {
        customizeEnvironment(propertySources);
      }
    });
  }
}
