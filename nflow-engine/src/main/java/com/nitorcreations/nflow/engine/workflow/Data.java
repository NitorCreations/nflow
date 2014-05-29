package com.nitorcreations.nflow.engine.workflow;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Data {
  public String value();
  boolean readOnly() default false;
  boolean instantiateNull() default false;
}
