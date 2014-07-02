package com.nitorcreations.nflow.engine.internal.dao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import java.io.IOException;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={DaoTestConfiguration.class})
@ActiveProfiles("nflow.db.h2")
public abstract class BaseDaoTest extends BaseNflowTest {

  @Inject
  protected DataSource ds;

  @After
  public void truncateDb() throws IOException {
    ResourceDatabasePopulator populator = populator();
    populator.addScript(new ClassPathResource("scripts/db/h2.truncate.sql"));
    execute(populator, ds);
  }

  private static ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

  private static Resource getSqlResource(String fileName) throws IOException {
    return new ByteArrayResource(readAllBytes(Paths.get("src", "main", "resources", "scripts", "db", fileName)));
  }
}
