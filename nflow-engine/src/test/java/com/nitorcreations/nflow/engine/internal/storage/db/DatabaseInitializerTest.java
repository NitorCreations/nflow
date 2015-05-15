package com.nitorcreations.nflow.engine.internal.storage.db;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.internal.dao.DaoTestConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DaoTestConfiguration.class })
@ActiveProfiles("nflow.db.h2")
@DirtiesContext
public class DatabaseInitializerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Inject
  DataSource ds;

  DatabaseInitializer initializer;

  @Test
  public void databaseCreationSkipWorks() {
    initializer = new DatabaseInitializer("a2", ds, environmentCreateOnStartup("false"));
  }

  @Test
  public void databaseCreationDoesNotThrowExceptionWhenDatabaseIsAlreadyCreated() {
    initializer = new DatabaseInitializer("fails", ds, environmentCreateOnStartup("true"));
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
