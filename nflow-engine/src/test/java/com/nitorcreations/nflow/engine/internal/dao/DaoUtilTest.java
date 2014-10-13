package com.nitorcreations.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

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
