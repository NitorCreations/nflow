package io.nflow.engine.processing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface NflowWorkflow {
    String name();

    String description();

    String defaultStartState();

    String errorState();

    /**
     * Minimum delay on execution retry after an error. Unit is milliseconds.
     */
    int minErrorTransitionDelay() default -1;
    /**
     * Maximum delay on execution retry after an error. Unit is milliseconds.
     */
    int maxErrorTransitionDelay() default -1;
    /**
     * Length of forced delay to break execution of a step that is considered to be busy looping. Unit is milliseconds.
     */
    int shortTransitionDelay() default -1;
    /**
     * Immediate transition delay.
     */
    int immediateTransitionDelay() default -1;
    /**
     * Maximum retry attempts.
     */
    int maxRetries() default -1;
    /**
     * Maximum number of subsequent state executions before forcing a short transition delay.
     */
    int maxSubsequentStateExecutions() default -1;
    /**
     * Delay after which workflow instance history (actions, states) can be deleted from database by nFlow.
     * Unit is hours.
     */
    int historyDeletableAfterHours() default -1;

    // TODO WorkflowSettings.deleteHistoryCondition
}
