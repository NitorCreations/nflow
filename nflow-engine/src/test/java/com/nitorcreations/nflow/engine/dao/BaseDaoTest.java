package com.nitorcreations.nflow.engine.dao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import java.io.IOException;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.BaseNflowTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={DaoTestConfiguration.class})
public abstract class BaseDaoTest extends BaseNflowTest {

  @Inject
  protected DataSource ds;

  @Before
  public void initDb() throws IOException {
    ResourceDatabasePopulator populator = populator();
    populator.addScript(getSqlResource("create_nflow_db.sql"));
    execute(populator, ds);
  }

  @After
  public void dropDb() throws IOException {
    ResourceDatabasePopulator populator = populator();
    populator.addScript(getSqlResource("drop_nflow_db.sql"));
    execute(populator, ds);
  }

  private static ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

  private static Resource getSqlResource(String fileName) throws IOException {
    String sql = new String(readAllBytes(Paths.get("src", "main", "resources", "scripts", fileName)), UTF_8);
    sql = sql.replaceAll(" unsigned ", " ")
        .replaceAll(" enum *\\([^)]*\\)", " varchar(30)")
        .replaceAll("on update current_timestamp", "");
    return new ByteArrayResource(sql.getBytes(UTF_8));
  }

}
