package io.nflow.tests.extension;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkipTestMethodsAfterFirstFailureExtension implements BeforeAllCallback,
        ExecutionCondition, AfterTestExecutionCallback {
    private static Logger logger = LoggerFactory.getLogger(SkipTestMethodsAfterFirstFailureExtension.class);

    private Class<?> testClass;
    private boolean hasFailed = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        testClass = context.getRequiredTestClass();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (hasFailed) {
            return ConditionEvaluationResult.disabled("Disabled because earlier test failed");
        }
        return ConditionEvaluationResult.enabled("");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            hasFailed = true;
            logger.info("test {} failed. Skipping rest of the test in class {}",
                    context.getRequiredTestMethod().getName(),
                    testClass.getSimpleName());
        }
    }
}
