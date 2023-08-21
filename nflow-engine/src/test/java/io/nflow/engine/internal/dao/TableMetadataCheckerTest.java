package io.nflow.engine.internal.dao;

import static io.nflow.engine.config.Profiles.H2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.nflow.engine.internal.storage.db.DatabaseInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DaoTestConfiguration.class })
@ActiveProfiles(H2)
@DirtiesContext
public class TableMetadataCheckerTest {
  @Inject
  private DataSource dataSource;
  @Inject
  private TableMetadataChecker tableMetadataChecker;
  private static DatabaseInitializer initializer = null;

  @BeforeEach
  public void setup() {
    if (initializer == null) {
      initializer = new DatabaseInitializer("metadata", dataSource, environmentCreateOnStartup("true"), ";");
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
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> tableMetadataChecker.ensureCopyingPossible("base", "fewer_columns"));
    assertThat(thrown.getMessage(), containsString("Source table base has more columns than destination table fewer_columns"));
  }

  @Test
  public void destinationWithMissingColumnsIsInvalid() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> tableMetadataChecker.ensureCopyingPossible("base", "wrong_columns"));
    assertThat(thrown.getMessage(), containsString("Destination table wrong_columns is missing columns [TEXT2] that are present in source table base"));
  }

  @Test
  public void destinationWithWrongTypeIsInvalid() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> tableMetadataChecker.ensureCopyingPossible("base", "wrong_type"));
    assertThat(thrown.getMessage(), containsString("Source column base.TIME1 has type TIME and destination column wrong_type.TIME1 has mismatching type INTEGER"));
  }

  @Test
  public void destinationWithSmallerColumnIsInvalid() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> tableMetadataChecker.ensureCopyingPossible("base", "smaller_size"));
    assertThat(thrown.getMessage(), containsString("Source column base.TEXT2 has size 30 and destination column smaller_size.TEXT2 smaller size 25"));
  }

  private MockEnvironment environmentCreateOnStartup(String value) {
    return new MockEnvironment().withProperty("nflow.db.create_on_startup", value);
  }
}
