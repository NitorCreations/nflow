package io.nflow.engine.workflow.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a state method throws an exception annotated with this annotation, it is not retried by nFlow engine. Instead, the
 * workflow instance is moved to failure state.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NonRetryable {
  // marker interface
}
