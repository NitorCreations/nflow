package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MysqlDatabaseConfigurationTest {

  MysqlDatabaseConfiguration.MySQLVariants variants = new MysqlDatabaseConfiguration.MySQLVariants();

  @Test
  public void leastWorks() {
    assertThat(variants.least("A", "B"), is("(case when A is null then B when B is null then A when A < B then A else B end)"));
  }
}
