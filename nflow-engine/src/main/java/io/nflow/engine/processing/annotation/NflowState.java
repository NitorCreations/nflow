package io.nflow.engine.processing.annotation;

import io.nflow.engine.workflow.definition.WorkflowStateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface NflowState {

    WorkflowStateType type() default WorkflowStateType.normal;

    /**
     *
     * @return
     */
    String displayName() default "";

    /**
     *
     * @return
     */
    String description()  default "";

    String[] nextStates() default {};

    String failureState() default "";

    /**
     * Maximum number of subsequent state executions before forcing a short transition delay, per state.
     */
    int maxSubsequentExecutions() default -1;

    /**
     * Maximum retry attempts.
     */
    int maxRetries() default -1;
}
