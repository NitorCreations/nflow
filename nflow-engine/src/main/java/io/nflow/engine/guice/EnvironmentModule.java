package io.nflow.engine.guice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import com.google.inject.AbstractModule;

public class EnvironmentModule extends AbstractModule {

  private final Properties userProperties;
  private final String[] classpathPropertiesFiles;

  protected EnvironmentModule(Properties userProperties, String... classpathPropertiesFiles) {
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
        try (InputStream inputStream = engineProperties.getInputStream()) {
          p.load(inputStream);
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
      protected void customizePropertySources(@NonNull MutablePropertySources propertySources) {
        customizeEnvironment(propertySources);
      }
    });
  }
}
