package com.nitorcreations.nflow.engine.internal.dao;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.internal.storage.db.DatabaseInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DaoTestConfiguration.class })
@ActiveProfiles("nflow.db.h2")
@DirtiesContext
public class TableMetadataCheckerTest {
  @Inject
  private DataSource dataSource;
  @Inject
  private TableMetadataChecker tableMetadataChecker;
  private static DatabaseInitializer initializer = null;
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    if (initializer == null) {
      initializer = new DatabaseInitializer("metadata", dataSource, environmentCreateOnStartup("true"));
    }
  }

  @Test
  public void identicalTableIsValid() {
    tableMetadataChecker.ensureCopyingPossible("base", "identical");
  }

  @Test
  public void tableIsValidWithItself() {
    tableMetadataChecker.ensureCopyingPossible("base", "base");
  }

  @Test
  public void destinationWithExtraColumnsIsValid() {
    tableMetadataChecker.ensureCopyingPossible("base", "more_columns");
  }

  @Test
  public void destinationWithLargerColumnIsValid() {
    tableMetadataChecker.ensureCopyingPossible("base", "larger_size");
  }

  @Test
  public void destinationWithFewerColumnsIsInvalid() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Source table base has more columns than destination table fewer_columns");
    tableMetadataChecker.ensureCopyingPossible("base", "fewer_columns");
  }

  @Test
  public void destinationWithMissingColumnsIsInvalid() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Destination table wrong_columns is missing columns [TEXT2] that are present in source table base");
    tableMetadataChecker.ensureCopyingPossible("base", "wrong_columns");
  }

  @Test
  public void destinationWithWrongTypeIsInvalid() {
    thrown.expect(IllegalArgumentException.class);
    thrown
        .expectMessage("Source column base.TIME1 has type TIME and destination column wrong_type.TIME1 has mismatching type INTEGER");
    tableMetadataChecker.ensureCopyingPossible("base", "wrong_type");
  }

  @Test
  public void destinationWithSmallerColumnIsInvalid() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Source column base.TEXT2 has size 30 and destination column smaller_size.TEXT2 smaller size 25");
    tableMetadataChecker.ensureCopyingPossible("base", "smaller_size");
  }

  private MockEnvironment environmentCreateOnStartup(String value) {
    return new MockEnvironment().withProperty("nflow.db.create_on_startup", value);
  }
}
