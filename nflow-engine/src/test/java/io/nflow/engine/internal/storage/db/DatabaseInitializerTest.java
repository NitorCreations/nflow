package io.nflow.engine.internal.storage.db;

import static io.nflow.engine.config.Profiles.H2;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.nflow.engine.internal.dao.DaoTestConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DaoTestConfiguration.class })
@ActiveProfiles(H2)
@DirtiesContext
public class DatabaseInitializerTest {

  @Inject
  DataSource ds;

  @Test
  public void databaseCreationSkipWorks() {
    new DatabaseInitializer("a2", ds, environmentCreateOnStartup("false"), ";");
  }

  @Test
  public void databaseCreationDoesNotThrowExceptionWhenDatabaseIsAlreadyCreated() {
    new DatabaseInitializer("fails", ds, environmentCreateOnStartup("true"), ";");
  }

  @Test
  public void unsupportedDatabaseTypeIdentified() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new DatabaseInitializer("a2", ds, environmentCreateOnStartup("true"), ";"));
    assertThat(thrown.getMessage(), containsString("No ddl script found"));
  }

  private MockEnvironment environmentCreateOnStartup(String value) {
    return new MockEnvironment().withProperty("nflow.db.create_on_startup", value);
  }
}
