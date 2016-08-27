package io.nflow.tests.demo;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SpringApplicationContext implements ApplicationContextAware {

  static ApplicationContext applicationContext;

  @Override
  @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "test code")
  public void setApplicationContext(ApplicationContext applicationContext) {
    SpringApplicationContext.applicationContext = applicationContext;
  }
}
