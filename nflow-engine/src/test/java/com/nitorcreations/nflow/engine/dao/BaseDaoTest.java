package com.nitorcreations.nflow.engine.dao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.BaseNflowTest;
import com.nitorcreations.nflow.engine.db.migrations.DatabaseSchemaMigrator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={DaoTestConfiguration.class})
@ActiveProfiles("nflow.db.h2")
public abstract class BaseDaoTest extends BaseNflowTest {
  static final Logger log = LoggerFactory.getLogger(BaseDaoTest.class);

  @Inject
  protected DataSource datasource;

  @Inject
  protected DatabaseSchemaMigrator migrator;

  @Before
  public void initDb() {
    migrator.migrateDatabaseSchema();
  }

  @After
  public void dropDb() {
    ResourceDatabasePopulator populator = populator();
    String sql = "DROP ALL OBJECTS";
    populator.addScript(new ByteArrayResource(sql.getBytes(UTF_8)));
    execute(populator, datasource);
  }

  private static ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

}
