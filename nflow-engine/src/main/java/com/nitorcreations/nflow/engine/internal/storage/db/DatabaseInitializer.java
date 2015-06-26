package com.nitorcreations.nflow.engine.internal.storage.db;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

public class DatabaseInitializer {
  private static final Logger logger = getLogger(DatabaseInitializer.class);
  private final String dbType;

  public DatabaseInitializer(String dbType, DataSource ds, Environment env) {
    this.dbType = dbType;
    if (!env.getRequiredProperty("nflow.db.create_on_startup", Boolean.class)) {
      return;
    }

    populate(createPopulator(resolveScript()), ds);
  }

  private void populate(ResourceDatabasePopulator populator, DataSource ds) {
    try {
      execute(populator, ds);
      logger.info("Database created.");
    } catch (ScriptStatementFailedException ex) {
      logger.warn("Failed to create the database, possibly already created: {}", ex.getMessage());
      logger.debug("Failed to create the database", ex);
    } catch (Exception ex) {
      logger.warn("Failed to create the database", ex);
    }
  }

  private ResourceDatabasePopulator createPopulator(ClassPathResource script) {
    logger.info("Creating database populator using script '{}'", script.getPath().toString());
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    populator.addScript(script);
    return populator;
  }

  private ClassPathResource resolveScript() {
    ClassPathResource script = new ClassPathResource("scripts/db/" + dbType + ".create.ddl.sql");
    if (!script.exists()) {
      throw new IllegalArgumentException("No ddl script found: " + script.toString());
    }
    return script;
  }
}
