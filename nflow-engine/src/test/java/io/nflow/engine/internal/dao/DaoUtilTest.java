package io.nflow.engine.internal.dao;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DaoUtilTest {

  @Test
  public void toDateTimeWorksForNull() {
    assertThat(DaoUtil.toDateTime(null), is(nullValue()));
  }

  @Test
  public void toTimestampWorksForNull() {
    assertThat(DaoUtil.toTimestamp(null), is(nullValue()));
  }
}
