package com.nitorcreations.nflow.engine.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseInitializer {
  private static final Logger logger = getLogger(DatabaseInitializer.class);

  public DatabaseInitializer(DataSource ds, Environment env) {
    if (!env.getRequiredProperty("nflow.db.create_on_startup", Boolean.class)) {
      return;
    }

    populate(createPopulator(resolveScript(env)), ds);
  }

  private void populate(ResourceDatabasePopulator populator, DataSource ds) {
    try {
      execute(populator, ds);
    } catch(Exception ex) {
      logger.warn("Failed to create the database - maybe it exists already", ex);
    }
  }

  private ResourceDatabasePopulator createPopulator(ClassPathResource script) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    populator.addScript(script);
    return populator;
  }

  private ClassPathResource resolveScript(Environment env) {
    String dbType = env.getRequiredProperty("nflow.db.type");
    ClassPathResource script = new ClassPathResource("scripts/db/" + dbType + ".create.ddl.sql");
    if (!script.exists()) {
      throw new IllegalArgumentException("Unsupported database type (nflow.db.type): " + dbType);
    }
    return script;
  }
}
