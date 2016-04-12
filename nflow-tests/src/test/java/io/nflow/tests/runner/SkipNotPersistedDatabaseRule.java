package io.nflow.tests.runner;

import static io.nflow.engine.internal.config.Profiles.MYSQL;
import static io.nflow.engine.internal.config.Profiles.ORACLE;
import static io.nflow.engine.internal.config.Profiles.POSTGRESQL;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.containsAny;
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
        if (!containsAny(getenv("SPRING_PROFILES_ACTIVE"), MYSQL, POSTGRESQL, ORACLE)) {
          assumeTrue("Skipped test for not real database", false);
        }
        base.evaluate();
      }
    };
  }

}