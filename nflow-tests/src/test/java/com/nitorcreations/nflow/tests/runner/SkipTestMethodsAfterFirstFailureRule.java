package com.nitorcreations.nflow.tests.runner;

import static org.junit.Assume.assumeTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Fails all tests in the same test class after the first failure has been detected.
 *
 * Normally you should request deterministic ordering of test methods by annotating the test class with
 * @FixMethodOrder or @FixMethodOrder(NAME_ASCENDING).
 */
public class SkipTestMethodsAfterFirstFailureRule implements TestRule {
  static final ConcurrentMap<Class<?>, String> failures = new ConcurrentHashMap<>();
  final Class<?> testClass;

  public SkipTestMethodsAfterFirstFailureRule(Class<?> testClass) {
    this.testClass = testClass;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        String failedTestName = failures.get(testClass);
        if (failedTestName != null) {
          assumeTrue("Previous test '" + failedTestName + "' failed", false);
        }
        boolean ok = false;
        try {
          base.evaluate();
          ok = true;
        } finally {
          if (!ok) {
            failures.put(testClass, description.getDisplayName());
          }
        }
      }
    };
  }
}