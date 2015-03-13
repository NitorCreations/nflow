package com.nitorcreations.nflow.engine.internal.storage.db;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseInitializerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  DataSource ds;

  DatabaseInitializer initializer;

  @Test
  public void databaseCreationSkipWorks() {
    initializer = new DatabaseInitializer("a2", ds, environmentCreateOnStartup("false"));
  }

  @Test
  public void unsupportedDatabaseTypeIdentified() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("No ddl script found");
    initializer = new DatabaseInitializer("a2", ds, environmentCreateOnStartup("true"));
  }

  private MockEnvironment environmentCreateOnStartup(String value) {
    return new MockEnvironment().withProperty("nflow.db.create_on_startup", value);
  }
}
