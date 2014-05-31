package com.nitorcreations.nflow.jetty.spring;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class NflowAnnotationConfigWebApplicationContext extends AnnotationConfigWebApplicationContext {
  private final ConfigurableEnvironment env;

  public NflowAnnotationConfigWebApplicationContext(ConfigurableEnvironment env) {
    this.env = env;
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return env;
  }
}
