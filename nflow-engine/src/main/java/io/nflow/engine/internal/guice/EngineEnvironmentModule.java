package io.nflow.engine.internal.guice;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import java.util.Properties;
import java.util.stream.Stream;

public class EngineEnvironmentModule extends EnvironmentModule {

  public EngineEnvironmentModule(final Properties userProperties, String... classpathPropertiesFiles) {
    super(userProperties, addDefaultPropertiesFiles("nflow-engine.properties", classpathPropertiesFiles));
  }

  protected static String[] addDefaultPropertiesFiles(String defaultPropertiesFile, String... classpathPropertiesFiles) {
    if (classpathPropertiesFiles == null) {
      return new String[] { defaultPropertiesFile };
    }
    return concat(Stream.of(defaultPropertiesFile), stream(classpathPropertiesFiles)).toArray(String[]::new);
  }
}
