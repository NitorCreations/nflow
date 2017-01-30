package io.nflow.engine.internal.guice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EngineEnvironmentModule extends EnvironmentModule {

  public EngineEnvironmentModule(final Properties userProperties, String... classpathPropertiesFiles) {
    super(userProperties, addDefaultPropertiesFiles("nflow-engine.properties", classpathPropertiesFiles));
  }

  protected static String[] addDefaultPropertiesFiles(String defaultPropertiesFile, String... classpathPropertiesFiles) {
    List<String> files = new ArrayList<>();
    files.add(defaultPropertiesFile);
    if (classpathPropertiesFiles != null) {
      files.addAll(Arrays.asList(classpathPropertiesFiles));
    }
    return files.toArray(new String[0]);
  }
}
