package io.nflow.engine.internal.storage.db;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import io.nflow.engine.config.NFlowConfiguration;

public class DatabaseInitializer {
  private static final Logger logger = getLogger(DatabaseInitializer.class);

  public DatabaseInitializer(String dbType, DataSource ds, NFlowConfiguration conf, String scriptSeparator) {
    if (!conf.getRequiredProperty("nflow.db.create_on_startup", Boolean.class)) {
      return;
    }
    populate(dbType, scriptSeparator, ds);
  }

  private void populate(String dbType, String scriptSeparator, DataSource ds) {
    ResourceDatabasePopulator populator = createPopulator(dbType, scriptSeparator);
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

  private ResourceDatabasePopulator createPopulator(String dbType, String scriptSeparator) {
    ClassPathResource script = resolveScript(dbType);
    logger.info("Creating database populator using script '{}'", script.getPath());
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSeparator(scriptSeparator);
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    populator.addScript(script);
    return populator;
  }

  private ClassPathResource resolveScript(String dbType) {
    ClassPathResource script = new ClassPathResource("scripts/db/" + dbType + ".create.ddl.sql");
    if (!script.exists()) {
      throw new IllegalArgumentException("No ddl script found: " + script);
    }
    return script;
  }
}
