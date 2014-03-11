package com.nitorcreations.nflow.jetty.spring;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class NflowAnnotationConfigWebApplicationContext extends AnnotationConfigWebApplicationContext {

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new NflowStandardEnvironment();
  }

}
