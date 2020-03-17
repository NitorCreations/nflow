package io.nflow.engine.workflow.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a state method throws an exception annotated with this annotation, the state method is not retried by nFlow engine.
 * Instead, the workflow instance is moved to failure state. This functionality may be overridden by defining a custom logic for
 * retryable exceptions via <code>WorkflowSettings</code> of the workflow definition.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface NonRetryableError {
  // marker interface
}
