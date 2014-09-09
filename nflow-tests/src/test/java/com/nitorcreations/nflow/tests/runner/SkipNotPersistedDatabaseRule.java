package com.nitorcreations.nflow.tests.runner;

import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.junit.Assume.assumeTrue;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SkipNotPersistedDatabaseRule implements TestRule {

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (!contains(getenv("SPRING_PROFILES_ACTIVE"), "nflow.db.mysql") &&
                !contains(getenv("SPRING_PROFILES_ACTIVE"), "nflow.db.postgresql")) {
          assumeTrue("Skipped test for not real database", false);
        }
        base.evaluate();
      }
    };
  }

}