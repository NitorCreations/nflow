package com.nitorcreations.nflow.engine.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.inject.Named;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseInitializer {

  private static final Logger logger = getLogger(DatabaseInitializer.class);

  public DatabaseInitializer(@Named("nflow-datasource") DataSource ds, Environment env) {
    ResourceDatabasePopulator populator = populator();
    if (!env.getRequiredProperty("db.create.on.startup", Boolean.class)) {
      return;
    }
    String dbType = env.getRequiredProperty("db.type");
    ClassPathResource script = new ClassPathResource("scripts/db/" + dbType + ".create.ddl.sql");
    if (!script.exists()) {
      throw new IllegalArgumentException("Unsupported database type (db.type): " + dbType);
    }
    populator.addScript(script);
    try {
      execute(populator, ds);
    } catch(Exception ex) {
      logger.warn("Failed to create the database - maybe it exists already", ex);
    }
  }

  private ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

}
