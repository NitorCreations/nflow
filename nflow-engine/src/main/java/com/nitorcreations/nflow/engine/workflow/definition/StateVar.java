package com.nitorcreations.nflow.engine.workflow.definition;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used for state handler method parameters. Annotated parameters are
 * automatically populated from the workflow instance variables and persisted
 * after processing the state handler method.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface StateVar {
  /**
   * The name of the workflow instance variable.
   *
   * @return The name of the variable.
   */
  public String value();

  /**
   * Makes the variable readonly in the state handler method.
   *
   * @return True when variable should not be modified, false otherwise.
   */
  boolean readOnly() default false;

  /**
   * Initializes the variable using default constructor (or zero value for
   * primitive types) if the variable does not exist. Used to ensure that the
   * variable value is never null.
   *
   * @return True if non-existing variables should be initialized, false otherwise.
   */
  boolean instantiateIfNotExists() default false;
}
